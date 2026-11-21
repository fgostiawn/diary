<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/67190f27-898e-4e3e-bcc3-18a1aec7bce9

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## Zine Kita (scrapbook digital)

`scrapbook/index.html` adalah scrapbook 20 halaman bertema anniversary yang berjalan langsung di browser, tanpa instalasi.

1. Buka `scrapbook/index.html` di Chrome, Edge, atau Safari.
2. **Mode edit**: klik tulisan untuk mengubah. Ketuk bingkai foto untuk membuka menu: **Pilih dari galeri**, **Ambil dengan kamera**, **Atur posisi foto**, atau **Hapus foto**. **Isi banyak foto** memilih banyak foto sekaligus dari galeri HP dan mengisi bingkai kosong berurutan. Di laptop, foto juga bisa diseret ke bingkai atau ditempel dengan Ctrl/Cmd+V. Foto HEIC dari iPhone dikonversi otomatis (butuh internet). Semua tersimpan otomatis di browser.
3. Pilih **Lanskap** (2 halaman per lembar A4, bisa dilipat jadi zine) atau **Potret** (1 halaman per lembar A4).
4. **Cetak / PDF**: pilih kertas A4, margin *None*, dan centang *Background graphics*.
5. **Bagikan file**: mengunduh `zine-kita.html` berisi semua foto dan tulisan. Penerima cukup membukanya di browser.
