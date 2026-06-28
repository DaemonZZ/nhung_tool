# Huong dan release ReconCore

Tai lieu nay danh cho nguoi build ban phat hanh app.

## Yeu cau moi truong

- JDK 17 co san cong cu `jpackage`.
- Build tren he dieu hanh nao thi tao goi native cho he dieu hanh do:
  - macOS: `.app` va `.dmg`
  - Windows: `.exe` hoac `.msi`
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

- Zip distribution: `build/distributions/nhung_tool-0.1.0.zip`
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

MacOS mac dinh tao `.dmg`. Windows mac dinh tao `.exe`. Linux mac dinh tao `.deb`.

Co the chi dinh loai installer bang property `installerType`:

```bash
./gradlew packageNativeInstaller -PinstallerType=dmg
./gradlew packageNativeInstaller -PinstallerType=pkg
./gradlew packageNativeInstaller -PinstallerType=exe
./gradlew packageNativeInstaller -PinstallerType=msi
./gradlew packageNativeInstaller -PinstallerType=deb
./gradlew packageNativeInstaller -PinstallerType=rpm
```

Neu project version dang la `0.x.x`, `jpackage` tren macOS khong chap nhan version bat dau bang `0`. Build script se mac dinh dung native package version `1.0.0`. Co the override khi can:

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

- Goi native build tren macOS khong tu dong duoc Apple notarize/sign. Neu gui ra ngoai cong ty, Gatekeeper co the hien canh bao khi mo lan dau.
- JavaFX dependency trong release la dependency theo he dieu hanh/kien truc may build. Can build rieng tren may Windows neu muon phat hanh ban Windows.

## Release len GitHub

Repository da co workflow `.github/workflows/release-installers.yml` de build installer tren GitHub Actions:

- macOS runner tao file `.dmg`
- Windows runner tao file `.exe`
- Neu push len `main`, workflow se build installer va luu trong artifact cua GitHub Actions.
- Neu workflow chay tu tag `v*`, cac installer se duoc upload vao GitHub Release tuong ung.

Cach release khuyen dung:

```bash
git tag v0.1.0
git push origin main
git push origin v0.1.0
```

Sau khi tag duoc push, vao tab Actions tren GitHub de theo doi workflow `Release Installers`. Khi workflow xong, GitHub Release se co:

- `ReconCore-macOS-v0.1.0.dmg`
- `ReconCore-Windows-v0.1.0.exe`

Co the chay thu cong tu GitHub Actions bang `workflow_dispatch`. Neu dien input `tag`, workflow se tao hoac cap nhat release theo tag do. Neu de trong `tag`, workflow chi build artifact de tai trong trang Actions, khong publish GitHub Release.

Neu chi muon test build tren GitHub ma chua tao release, push commit len `main`:

```bash
git push origin main
```

Luc nay workflow se chay va artifact installer se nam trong trang run cua GitHub Actions, nhung chua tao GitHub Release.
