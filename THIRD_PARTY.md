# Android Xray core

The bundled `app/libs/libv2ray.aar` is the upstream AndroidLibXrayLite v26.11.9 release (LGPL-3.0).

- Binary: https://github.com/2dust/AndroidLibXrayLite/releases/tag/v26.11.9
- Corresponding wrapper source: `third_party/AndroidLibXrayLite-v26.11.9-source.zip`
- License: `third_party/AndroidLibXrayLite-LGPL-3.0.txt`
- Xray source and dependency versions are pinned in that source archive's `go.mod` and `go.sum`.
- SHA-256 of the unmodified AAR: `cdfef146218233a31a8b52c8b1016557730d3b63fc121d4c84cacea50e669155`

Rebuild instructions and upstream build workflows are in the corresponding source archive. Replace the AAR and rebuild the app to use a modified library. The app remains GPL-3.0-or-later; see LICENSE and upstream notices.
