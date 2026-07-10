# Huong dan release ReconCore

Tai lieu nay danh cho nguoi build ban phat hanh app.

## Yeu cau moi truong

- JDK 17 co san cong cu `jpackage`.
- Build tren he dieu hanh nao thi tao goi native cho he dieu hanh do:
  - macOS: `.app` va `.dmg`
  - Windows: `.msi`
  - Linux: `.deb` hoac `.rpm`
- Khong can dua cac file Excel dau vao vao release. App se cho nguoi dung chon file khi chay.

## Build ban release co ban

Chay lenh:

```bash
./gradlew clean release
```

Lenh nay se:

- Chay test.
- Tao file zip distribution.
- Tao native app image bang `jpackage`.

Artifact dau ra:

- Zip distribution: `build/distributions/nhung_tool-0.1.3.zip`
- Native app image: `build/jpackage/image/ReconCore.app` tren macOS, hoac thu muc app tuong ung tren he dieu hanh hien tai.

## Build installer

Chay lenh:

```bash
./gradlew packageNativeInstaller
```

Artifact dau ra nam trong:

```text
build/jpackage/installer
```

MacOS mac dinh tao `.dmg`. Windows mac dinh tao `.msi`. Linux mac dinh tao `.deb`.

Co the chi dinh loai installer bang property `installerType`:

```bash
./gradlew packageNativeInstaller -PinstallerType=dmg
./gradlew packageNativeInstaller -PinstallerType=pkg
./gradlew packageNativeInstaller -PinstallerType=exe
./gradlew packageNativeInstaller -PinstallerType=msi
./gradlew packageNativeInstaller -PinstallerType=deb
./gradlew packageNativeInstaller -PinstallerType=rpm
```

Neu project version dang la `0.x.x`, `jpackage` tren macOS khong chap nhan version bat dau bang `0`. Build script se doi so version dau tien thanh `1` cho native package. Vi du project `0.1.3` se duoc dong goi native version `1.1.3`. Co the override khi can:

```bash
./gradlew packageNativeInstaller -PnativeAppVersion=1.0.1
```

## Kiem tra nhanh truoc khi gui khach hang

1. Chay app tu native image.
2. Chon file hoa don va file xuat nhap ton mau.
3. Kiem tra cac man Input, Mapping review, Unit review, Negative inventory va Export.
4. Xuat ket qua va xac nhan folder export co 2 nhom file: dong hop le/da duyet va dong can xu ly rieng.
5. Mo lai file Excel dau vao de dam bao app khong lam thay doi noi dung file goc.

## Luu y ky phat hanh

- Goi native build tren macOS can duoc sign va notarize bang Apple Developer ID neu gui cho khach hang. Neu khong, Gatekeeper co the hien loi `"ReconCore" Not Opened`.
- JavaFX dependency trong release la dependency theo he dieu hanh/kien truc may build. Can build rieng tren may Windows neu muon phat hanh ban Windows.

## Xu ly loi macOS Gatekeeper

Neu macOS bao loi:

```text
"ReconCore" Not Opened
Apple could not verify "ReconCore" is free of malware...
```

Nguyen nhan la file `.app` hoac `.dmg` chua duoc Apple notarize. Cach dung cho noi bo/test nhanh:

```bash
xattr -dr com.apple.quarantine /Applications/ReconCore.app
```

Khong nen dung workaround nay cho ban release gui khach hang. Ban release chinh thuc can duoc sign va notarize trong GitHub Actions.

Can cau hinh cac GitHub repository secrets sau:

- `MACOS_CERTIFICATE_P12_BASE64`: file Developer ID Application `.p12` da encode base64.
- `MACOS_CERTIFICATE_PASSWORD`: mat khau cua file `.p12`.
- `MACOS_SIGNING_KEY_USER_NAME`: phan ten/team trong signing identity, vi du `Ten Cong Ty (TEAMID)`. Khong them prefix `Developer ID Application:`.
- `APPLE_API_KEY_ID`: Apple App Store Connect API key ID.
- `APPLE_API_ISSUER_ID`: Apple App Store Connect issuer ID.
- `APPLE_API_KEY_P8_BASE64`: file `AuthKey_XXXX.p8` da encode base64.

Lenh encode base64 tren macOS:

```bash
base64 -i DeveloperIDApplication.p12 | pbcopy
base64 -i AuthKey_XXXX.p8 | pbcopy
```

Sau khi them secrets, push tag release moi. Workflow se sign, notarize, staple DMG, roi upload len GitHub Release.

## Xu ly canh bao Windows installer

Neu Windows hien hop thoai:

```text
This program might not have installed correctly
```

Day la canh bao cua Program Compatibility Assistant, khong phai loi crash cua app. Tu ban `v0.1.3`, Windows release dung `.msi` thay cho `.exe` va installer co them Start Menu, desktop shortcut, per-user install va upgrade UUID de giam kha nang Windows hien canh bao nay.

Neu app da cai va mo duoc, chon:

```text
This program installed correctly
```

Neu van canh bao khi tai tu Internet, do installer chua duoc code-sign bang chung thu Windows. Khi phat hanh chinh thuc cho nhieu khach hang, nen mua code-signing certificate cho Windows.

## Release len GitHub

Repository da co workflow `.github/workflows/release-installers.yml` de build installer tren GitHub Actions:

- macOS runner tao file `.dmg`
- Windows runner tao file `.msi`
- Neu push len `main`, workflow se build installer va luu trong artifact cua GitHub Actions.
- Neu workflow chay tu tag `v*`, cac installer se duoc upload vao GitHub Release tuong ung.

Cach release khuyen dung:

```bash
git tag v0.1.3
git push origin main
git push origin v0.1.3
```

Sau khi tag duoc push, vao tab Actions tren GitHub de theo doi workflow `Release Installers`. Khi workflow xong, GitHub Release se co:

- `ReconCore-macOS-v0.1.3.dmg`
- `ReconCore-Windows-v0.1.3.msi`

Co the chay thu cong tu GitHub Actions bang `workflow_dispatch`. Neu dien input `tag`, workflow se tao hoac cap nhat release theo tag do. Neu de trong `tag`, workflow chi build artifact de tai trong trang Actions, khong publish GitHub Release.

Neu chi muon test build tren GitHub ma chua tao release, push commit len `main`:

```bash
git push origin main
```

Luc nay workflow se chay va artifact installer se nam trong trang run cua GitHub Actions, nhung chua tao GitHub Release.
