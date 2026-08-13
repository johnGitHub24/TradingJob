# CodeGraphic image export

Source: docs/codeGraphic.html
Tool: @mermaid-js/mermaid-cli@11 (dark)
Script: EngineeringOS/eos-minimal/hooks/export-codeGraphic-images.ps1

| File | Tab |
|------|-----|
| `01-joba.svg` / `.png` | JOB-A |
| `02-jobb.svg` / `.png` | JOB-B |
| `03-jobcd.svg` / `.png` | JOB-C/D |
| `04-runtime.svg` / `.png` | Runtime |

Re-run from project root (relative hook; do not use ClaudeCode / start.ps1):

    & "..\EngineeringOS\eos-minimal\hooks\export-codeGraphic-images.ps1" -ProjectRoot .
