; WallKraft NSIS installer
; Build: makensis /DVERSION=<semver> /DEXE_PATH=<path\to\wallkraft.exe> installer.nsi
; Per-user install (no admin required). Writes to %LOCALAPPDATA%\Programs\WallKraft.

!define APP_NAME "WallKraft"
!define APP_PUBLISHER "Kedhar Sairam"
!define APP_EXE "wallkraft.exe"
!define UNINSTALL_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\WallKraft"

!ifndef VERSION
  !define VERSION "0.1.0"
!endif
!ifndef EXE_PATH
  !error "EXE_PATH must be set: makensis /DEXE_PATH=..."
!endif

Unicode true
RequestExecutionLevel user
SetCompressor /SOLID lzma
Name "${APP_NAME} ${VERSION}"
OutFile "target\release\WallKraft-${VERSION}-setup.exe"
InstallDir "$LOCALAPPDATA\Programs\WallKraft"

; Version info for the installer itself (x.x.x.x format required)
VIProductVersion "${VERSION}.0"
VIAddVersionKey "ProductName" "${APP_NAME}"
VIAddVersionKey "FileDescription" "${APP_NAME} Installer"
VIAddVersionKey "FileVersion" "${VERSION}"
VIAddVersionKey "ProductVersion" "${VERSION}"
VIAddVersionKey "LegalCopyright" "Copyright (c) 2026 ${APP_PUBLISHER}"

!include "MUI2.nsh"
!define MUI_ABORTWARNING
!define MUI_ICON "assets\wallkraft.ico"
!define MUI_UNICON "assets\wallkraft.ico"
!define MUI_FINISHPAGE_RUN "$INSTDIR\${APP_EXE}"
!define MUI_FINISHPAGE_RUN_TEXT "Launch ${APP_NAME}"
!define MUI_FINISHPAGE_RUN_PARAMETERS ""

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Section "Install"
  SetOutPath "$INSTDIR"
  File "${EXE_PATH}"
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ; Start Menu
  CreateDirectory "$SMPROGRAMS\WallKraft"
  CreateShortcut "$SMPROGRAMS\WallKraft\WallKraft.lnk" "$INSTDIR\${APP_EXE}"
  CreateShortcut "$SMPROGRAMS\WallKraft\Uninstall WallKraft.lnk" "$INSTDIR\Uninstall.exe"

  ; Add/Remove Programs
  WriteRegStr HKCU "${UNINSTALL_KEY}" "DisplayName" "${APP_NAME}"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "Publisher" "${APP_PUBLISHER}"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "DisplayIcon" "$INSTDIR\${APP_EXE}"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "${UNINSTALL_KEY}" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegStr HKCU "${UNINSTALL_KEY}" "QuietUninstallString" '"$INSTDIR\Uninstall.exe" /S'
  WriteRegDWORD HKCU "${UNINSTALL_KEY}" "NoModify" 1
  WriteRegDWORD HKCU "${UNINSTALL_KEY}" "NoRepair" 1
SectionEnd

Section "Uninstall"
  Delete "$INSTDIR\${APP_EXE}"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir "$INSTDIR"

  Delete "$SMPROGRAMS\WallKraft\WallKraft.lnk"
  Delete "$SMPROGRAMS\WallKraft\Uninstall WallKraft.lnk"
  RMDir "$SMPROGRAMS\WallKraft"

  DeleteRegKey HKCU "${UNINSTALL_KEY}"
SectionEnd
