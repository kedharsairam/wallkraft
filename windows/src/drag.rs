//! OS-level drag-out: drag a wallpaper tile onto the desktop / a folder to
//! save the full-res image there.
//!
//! Slint's built-in drag & drop only works between Slint elements, so we drive
//! the native OLE machinery directly: an `IDataObject` advertising `CF_HDROP`
//! plus an `IDropSource`, handed to `DoDragDrop`. The drag loop runs on the UI
//! thread (standard for OLE — it pumps the thread's message queue) and returns
//! once the user drops or cancels.

use std::path::{Path, PathBuf};

use windows::core::{implement, BOOL, Error, HRESULT, Ref, Result};
use windows::Win32::Foundation::{DRAGDROP_S_CANCEL, DRAGDROP_S_DROP, DV_E_FORMATETC, HGLOBAL, POINT};
use windows::Win32::System::Com::{
    CoTaskMemAlloc, FORMATETC, IAdviseSink, IDataObject, IDataObject_Impl, IEnumFORMATETC,
    IEnumSTATDATA, STGMEDIUM, STGMEDIUM_0, TYMED_HGLOBAL,
};
use windows::Win32::System::Ole::{
    DoDragDrop, IDropSource, IDropSource_Impl, CF_HDROP, DROPEFFECT, DROPEFFECT_COPY,
    OleInitialize,
};
use windows::Win32::System::SystemServices::{MK_LBUTTON, MODIFIERKEYS_FLAGS};
use windows::Win32::UI::Shell::DROPFILES;

/// E_NOTIMPL (0x80004001) — used by the stubbed `IDataObject` methods.
const E_NOTIMPL: HRESULT = HRESULT(0x80004001u32 as i32);
/// S_OK (0x00000000).
const S_OK: HRESULT = HRESULT(0);

/// Initialize OLE for this thread exactly once. Must run on the UI thread
/// before any drag-out. Failures (e.g. COM already initialized in a different
/// mode by the host) are non-fatal — `DoDragDrop` only needs *some* apartment.
pub fn ensure_ole() {
    use std::sync::Once;
    static ONCE: Once = Once::new();
    ONCE.call_once(|| unsafe {
        if let Err(e) = OleInitialize(None) {
            tracing::warn!("OleInitialize: {e}");
        }
    });
}

/// Start an OLE drag for `path`. Blocks until the user drops or cancels.
/// Returns true only if the drop actually happened (vs. cancelled with Esc).
pub fn start_drag(path: &Path) -> bool {
    let data_object: IDataObject = FileDataObject { path: path.to_path_buf() }.into();
    let drop_source: IDropSource = DropSource.into();
    let mut effect = DROPEFFECT(0);
    let hr = unsafe { DoDragDrop(&data_object, &drop_source, DROPEFFECT_COPY, &mut effect) };
    tracing::info!("drag ended: hresult=0x{:08x} effect={}", hr.0 as u32, effect.0);
    hr == DRAGDROP_S_DROP
}

/// A data object that only serves `CF_HDROP` (a file path) via HGLOBAL.
#[implement(IDataObject)]
struct FileDataObject {
    path: PathBuf,
}

impl IDataObject_Impl for FileDataObject_Impl {
    fn GetData(&self, pformatetcin: *const FORMATETC) -> Result<STGMEDIUM> {
        unsafe {
            let fmt = &*pformatetcin;
            if fmt.cfFormat == CF_HDROP.0 && fmt.tymed & TYMED_HGLOBAL.0 as u32 != 0 {
                let medium = STGMEDIUM {
                    tymed: TYMED_HGLOBAL.0 as u32,
                    u: STGMEDIUM_0 { hGlobal: build_hdrop(&self.path) },
                    // `pUnkForRelease` stays None so ReleaseStgMedium CoTaskMemFree's it.
                    pUnkForRelease: core::mem::ManuallyDrop::new(None),
                };
                Ok(medium)
            } else {
                Err(Error::from_hresult(DV_E_FORMATETC))
            }
        }
    }

    fn QueryGetData(&self, pformatetc: *const FORMATETC) -> HRESULT {
        unsafe {
            let fmt = &*pformatetc;
            if fmt.cfFormat == CF_HDROP.0 && fmt.tymed & TYMED_HGLOBAL.0 as u32 != 0 {
                S_OK
            } else {
                DV_E_FORMATETC
            }
        }
    }

    fn GetCanonicalFormatEtc(&self, _: *const FORMATETC, _: *mut FORMATETC) -> HRESULT {
        E_NOTIMPL
    }

    fn GetDataHere(&self, _: *const FORMATETC, _: *mut STGMEDIUM) -> Result<()> {
        Err(Error::from_hresult(E_NOTIMPL))
    }

    fn SetData(&self, _: *const FORMATETC, _: *const STGMEDIUM, _: BOOL) -> Result<()> {
        Err(Error::from_hresult(E_NOTIMPL))
    }

    fn EnumFormatEtc(&self, _: u32) -> Result<IEnumFORMATETC> {
        Err(Error::from_hresult(E_NOTIMPL))
    }

    fn DAdvise(&self, _: *const FORMATETC, _: u32, _: Ref<IAdviseSink>) -> Result<u32> {
        Err(Error::from_hresult(E_NOTIMPL))
    }

    fn DUnadvise(&self, _: u32) -> Result<()> {
        Err(Error::from_hresult(E_NOTIMPL))
    }

    fn EnumDAdvise(&self) -> Result<IEnumSTATDATA> {
        Err(Error::from_hresult(E_NOTIMPL))
    }
}

/// Build an HGLOBAL containing a `DROPFILES` struct followed by the UTF-16
/// path and a double NUL terminator, as required for `CF_HDROP`.
fn build_hdrop(path: &Path) -> HGLOBAL {
    let wide: Vec<u16> = path.to_string_lossy().encode_utf16().collect();
    let payload = std::mem::size_of::<DROPFILES>() + (wide.len() + 2) * 2;
    unsafe {
        let mem = CoTaskMemAlloc(payload);
        if mem.is_null() {
            return HGLOBAL(std::ptr::null_mut());
        }
        let dropfiles = mem as *mut DROPFILES;
        (*dropfiles).pFiles = std::mem::size_of::<DROPFILES>() as u32;
        (*dropfiles).pt = POINT { x: 0, y: 0 };
        (*dropfiles).fNC = BOOL(0);
        (*dropfiles).fWide = BOOL(1);
        let path_ptr = mem.add(std::mem::size_of::<DROPFILES>()) as *mut u16;
        std::ptr::copy_nonoverlapping(wide.as_ptr(), path_ptr, wide.len());
        *path_ptr.add(wide.len()) = 0;
        *path_ptr.add(wide.len() + 1) = 0;
        HGLOBAL(mem)
    }
}

/// Minimal drop source: cancel on Esc, drop when the mouse button is released.
#[implement(IDropSource)]
struct DropSource;

impl IDropSource_Impl for DropSource_Impl {
    fn QueryContinueDrag(&self, fescapepressed: BOOL, grfkeystate: MODIFIERKEYS_FLAGS) -> HRESULT {
        if fescapepressed.as_bool() {
            DRAGDROP_S_CANCEL
        } else if grfkeystate.0 & MK_LBUTTON.0 != 0 {
            S_OK // still dragging
        } else {
            DRAGDROP_S_DROP
        }
    }

    fn GiveFeedback(&self, _dweffect: DROPEFFECT) -> HRESULT {
        S_OK // use the default OLE drag cursors
    }
}
