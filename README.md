# DonutCore 🍩

![PaperMC](https://img.shields.io/badge/PaperMC-1.20+-blue?style=flat-square)
![Open Source](https://img.shields.io/badge/Open%20Source-100%25-success?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

**DonutCore** este un plugin core lightweight, 100% open-source și complet gratuit, dezvoltat special pentru serverele PaperMC. Inspirat de mecanicile populare de pe servere precum DonutSMP, acest plugin aduce un sistem modular avansat, perfect pentru gestionarea economiei și a recompenselor direct prin interfețe grafice (GUI).

## ✨ Caracteristici Principale

DonutCore este construit modular, permițându-ți să activezi doar funcționalitățile de care ai nevoie:

* **Sistem de Crates (Stil DonutSMP):** Cufere cu recompense dinamice, complet configurabile.
* **Management In-Game prin GUI:** * Creare, modificare și editare de chei direct din joc, fără a atinge fișierele de configurare.
    * Interfață intuitivă pentru setarea șanselor și a recompenselor.
* **Module Incluse:**
    * 📦 **Crate GUI:** Interfață principală pentru previzualizarea și deschiderea cutiilor.
    * 💰 **Sell GUI:** Un meniu rapid și eficient pentru vânzarea itemelor și integrare în economie.
    * 🌌 **Variable Enderchest GUI:** Enderchest-uri cu dimensiuni variabile și personalizabile în funcție de permisiunile jucătorului (ex: rânduri în plus pentru VIP-uri).
* **Lightweight & Optimizat:** Gândit să consume resurse minime, asigurând un TPS stabil pe serverul tău.

## 📥 Instalare

1. Descarcă ultima versiune a pluginului din secțiunea [Releases](../../releases).
2. Plasează fișierul `DonutCore-x.x.x.jar` în folderul `plugins/` al serverului tău.
3. Repornește serverul pentru a genera fișierele de configurare.
4. (Opțional) Editează fișierul `config.yml` și mesajele din folderul `plugins/DonutCore/` pentru a le adapta nevoilor tale.
5. Folosește comanda de reload in-game pentru a aplica modificările.

## 🛠️ Comenzi și Permisiuni

Mai jos găsești comenzile de bază ale pluginului. *(Poți personaliza permisiunile din config)*

| Comandă | Permisiune | Descriere |
| :--- | :--- | :--- |
| `/donutcore` | `donutcore.help` | Afișează lista de comenzi disponibile. |
| `/crates` | `donutcore.crates.use` | Deschide meniul principal pentru Crates. |
| `/sell` | `donutcore.sell.use` | Deschide interfața GUI pentru vânzarea de iteme. |
| `/ec` | `donutcore.enderchest.use` | Deschide Enderchest-ul variabil. |
| `/donutcore admin` | `donutcore.admin` | Acces la meniul GUI de editare/creare a cheilor și cutiilor. |
| `/donutcore reload`| `donutcore.admin` | Reîncarcă fișierele de configurare. |

## 🚀 Planuri de Viitor (Roadmap)

Fiind un proiect aflat în dezvoltare continuă, următoarele module sunt planificate pentru update-urile viitoare:
* [ ] Suport pentru baze de date (MySQL / MariaDB) pentru sincronizare cross-server.
* [ ] Modul de economie intern (sau suport extins pentru Vault).
* [ ] Sistem de quest-uri / misiuni zilnice integrate prin GUI.
* [ ] Mai multe opțiuni de personalizare vizuală pentru animațiile cufărelor.

## 🤝 Contribuții

Acest proiect este **100% Open Source**. Contribuțiile din partea comunității sunt extrem de apreciate! 
Dacă ai o idee de modul nou, vrei să optimizezi codul sau ai găsit un bug:
1. Fă un **Fork** acestui repository.
2. Creează un branch nou (`git checkout -b feature/ModulNou`).
3. Fă commit cu modificările tale (`git commit -m 'Am adăugat un modul nou'`).
4. Dă push pe branch-ul tău (`git push origin feature/ModulNou`).
5. Deschide un **Pull Request**.

## 🐛 Raportare Bug-uri

Dacă întâmpini probleme cu pluginul, te rugăm să deschizi un [Issue](../../issues) pe GitHub, atașând log-urile din consolă și pașii necesari pentru a reproduce problema.

## 📄 Licență

Acest proiect este distribuit sub licența **MIT**. Ești liber să îl folosești, să îl modifici și să îl distribui în mod gratuit. Vezi fișierul `LICENSE` pentru mai multe detalii.
