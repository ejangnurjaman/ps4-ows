# Changelog

## 1.0.1
- Added **Select Method** feature allowing users to choose between **Default Toolkit** and **Custom** hosting methods.
- **Default Toolkit Method**: Supports multiple official verified ZIP files with SHA-256 integrity verification:
  - `ps4ows.zip` (`3024070420f67d102d886cc9e785e36b70144693e50ca9ddee35c8a6dfdc1185`)
  - `ps4ows.rawgame4.v1.zip` (`5ad8785f53a6f565fd70c14a0791cc34fdf7798e1633ae7b2a8b520740701c15`)
  - `ps4ows.raw13g.v1.zip` (`7b000d912db10ed2cd638c30a9ac794a2d0e87394090396a1305c67e1c8fc16b`)
- **Custom Method**: Allows hosting web files directly from a custom folder named `ps4ows` in internal storage (`/sdcard/ps4ows`).
- Updated UI components, documentation, and error handling.

## 1.0.0-beta
- Initial release of PS4 OWS.
- Specialized HTTP server for PS4 offline hosting.
- Integrated SHA-256 file integrity verification.
