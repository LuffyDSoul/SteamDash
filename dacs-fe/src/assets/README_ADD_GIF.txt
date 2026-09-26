Please add the GIF file `lcpjZfMORLb.gif` to this folder so the app can use it as a local fallback for the Tenor embed.

Steps to add the file:
1. Download the GIF from Tenor: https://tenor.com/view/maxwell-gif-9262676895723379299 or directly from the GIF URL if available.
2. Save the file with the exact name `lcpjZfMORLb.gif` in this folder (`src/assets/`).
3. Rebuild or restart the dev server (ng serve) if it's running so Angular's dev server serves the new asset.

Notes:
- If you want transparency or a different format, consider converting the GIF to animated WebP or WebM with alpha and update the path in `compare.component.ts` to the new filename.
- This repository helper cannot download external files automatically due to environment limitations; please add the file manually or commit it to the repo.
