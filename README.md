# QR Toolkit

A native Android app that scans and generates QR codes and barcodes, with
smart follow-up actions for what a code actually contains and a real
custom-styled generator rather than a plain black-and-white square.

## What it does

- **Scan** — live camera scanning (CameraX + on-device Google ML Kit, works
  fully offline) or pick an existing photo to decode. Recognizes QR codes and
  common 1D/2D barcodes (Code 128/39/93, EAN-13/8, UPC-A/E, ITF, Codabar,
  PDF417, Aztec, Data Matrix). Each decoded code gets a smart action based on
  what it actually is:
  - **URL** → open in browser
  - **Wi-Fi network** → shows SSID/password/security with a copy button and
    a shortcut to Wi-Fi settings
  - **Contact card (vCard)** → add to contacts
  - **Email** → compose with address/subject/body pre-filled
  - **SMS** → compose with number/message pre-filled
  - **Phone number** → open dialer
  - **Location** → open in maps
  - **Calendar event** → add to calendar
  - **Retail barcode (EAN/UPC)** → optional product lookup (see below)
  - Anything else → raw text with copy / web-search actions
- **Generate** — build a QR code for nine different content types (plain
  text, URL, Wi-Fi, contact, email, SMS, phone, location, calendar event),
  each with its own form, not just a generic text box. Styling is genuinely
  customizable: foreground color, square-vs-rounded modules, and an optional
  logo dropped into the center — see "Custom QR styling" below for how that
  actually works. Save to the gallery or share directly.
- **History** — every decoded code, newest first, with copy/open actions and
  a clear-all.

## Why native Android (Kotlin), not cross-platform

Live camera-frame barcode decoding and QR generation both need direct access
to platform camera (CameraX) and on-device ML (Google ML Kit) APIs with no
first-class equivalent in cross-platform frameworks — using one would mean
writing a native plugin anyway. Going straight to Kotlin + Jetpack Compose
gives full, direct access with less indirection.

## Architecture

```
app/src/main/java/com/qrtoolkit/app/
├── data/
│   ├── model/              QrContentType, ScannedCode (persisted history
│   │                       entry), ScanDetails (sealed class of the
│   │                       structured fields behind a decode's smart
│   │                       actions -- deliberately not persisted)
│   ├── scan/
│   │   ├── BarcodeAnalyzer.kt       CameraX ImageAnalysis.Analyzer feeding
│   │   │                           frames to ML Kit's on-device decoder
│   │   ├── BarcodeMapper.kt         ML Kit Barcode -> this app's own
│   │   │                           ScanDetails/ScanContentKind
│   │   ├── SmartActionResolver.kt   ScanDetails -> Intent-based actions,
│   │   │                           none needing extra runtime permissions
│   │   └── ScanHistoryStore.kt      DataStore-backed local history
│   ├── generate/
│   │   ├── QrContentBuilder.kt   Builds the raw payload per content type
│   │   │                        (WIFI:/mailto:/smsto:/tel:/geo:/vCard/VEVENT)
│   │   ├── QrEncoder.kt          ZXing BitMatrix -> custom-styled Bitmap
│   │   └── QrExportUtils.kt      MediaStore save / FileProvider share
│   └── lookup/
│       └── ProductLookupService.kt  Optional Open Food Facts EAN/UPC lookup
├── di/AppContainer.kt    Small hand-rolled DI container (no Hilt needed at
│                         this size)
├── ui/                   Jetpack Compose screens + ViewModels, one package
│                         per feature (scan, generate, history), plus shared
│                         theme/components
└── navigation/           Bottom-nav Destinations + NavHost
```

`QrContentBuilder`, `BarcodeMapper`, and `QrEncoder`'s matrix math have no
Android framework dependency beyond `Bitmap`/`Canvas` for the encoder, so
the payload-formatting and mapping logic is straightforward to unit test.

## Custom QR styling

Most "QR generator" apps just render ZXing's default plain black squares.
This one renders the `BitMatrix` itself onto a `Canvas` (`QrEncoder.kt`), so:

- **Any foreground/background color**, not just black-on-white.
- **Square or rounded modules** — each "on" cell drawn as a filled rect or a
  circle.
- **An optional center logo.** When a logo is set, the error-correction
  level is bumped to `H` (~30% recoverable) specifically because the logo
  occludes real modules underneath it; at a lower level a logo-bearing code
  can become unscannable. The logo is capped at 20% of the code's width with
  a white backing plate so it stays legible against whatever modules sit
  behind it.

## Permissions

| Permission | Why |
|---|---|
| `CAMERA` | Live scanning. Requested only when the Scan tab is open, via `CameraPermissionGate` — Generate and History never need it. |
| `INTERNET` / `ACCESS_NETWORK_STATE` | The optional product-lookup call only. |
| `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28) | Only relevant on Android 9 and below; saving a generated QR to the gallery on API 29+ uses scoped-storage `MediaStore` inserts, which need no permission at all. |

No location, contacts, or calendar permission is ever requested. "Add to
contacts"/"add to calendar" hand off to those apps' own add-entry UI via
`Intent.ACTION_INSERT`/`ACTION_INSERT_OR_EDIT`, and "Call" opens the dialer
via `ACTION_DIAL` rather than dialing directly — none of that needs this
app to hold the underlying permission itself.

## Wi-Fi QR codes: what's deliberately not implemented

A decoded Wi-Fi QR shows the SSID/password/security and a shortcut to Wi-Fi
settings, but doesn't attempt to join the network automatically. Android
does have APIs for this (`WifiNetworkSuggestion`/`WifiNetworkSpecifier`,
API 29+), but their behavior is inconsistent enough across OEM Wi-Fi stacks
that showing the credentials for a quick manual connect is the more
honest, reliably-working default.

## Product lookup

Scanning a retail barcode (EAN/UPC) offers a "Look up product" button that
queries [Open Food Facts](https://world.openfoodfacts.org)' free, keyless
API — the one genuinely public product database that doesn't require
signing up for an API key. Coverage is food/grocery-focused, so a non-food
barcode will often come back with no match; that's a data-coverage gap, not
a bug.

## Building

Requires Android Studio (or the command-line Android SDK) with:
compileSdk/targetSdk 34, JDK 17.

```
./gradlew assembleDebug
```

Every push also builds automatically via GitHub Actions
(`.github/workflows/android-build.yml`), which uploads the debug APK as a
workflow artifact — see the repo's Actions tab for the latest build and a
downloadable APK without needing a local Android SDK at all.

## Minimum SDK

`minSdk 26` (Android 8.0), `targetSdk 34`.
