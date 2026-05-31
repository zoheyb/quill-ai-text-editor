# ✦ Quill — AI-Powered Text Editor

A modern desktop text editor built with Java Swing that integrates real-time AI assistance via the Groq API.

Built as an **Object-Oriented Programming** course project.

---

## Features

### Editor
- New, Open, Save, Save As file operations
- Cut, Copy, Paste, Undo, Redo (200-step history)
- Find & Replace dialog
- Line numbers panel
- Auto-indent & tab expansion (2/4 spaces)
- Word count, character count, line & column indicator
- Dark & Light theme toggle
- Font size adjustment
- HiDPI auto-scaling for high-resolution displays

### AI (Groq API + Llama 3.3 70B)
- **Fix & Rephrase** — corrects grammar and rewrites selected text professionally
- **Summarize** — generates a concise summary of selected text
- **Explain Code** — explains any code snippet in plain English

---


---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| GUI | Swing |
| HTTP Client | OkHttp 4.12 |
| JSON Parser | Gson 2.10 |
| AI Provider | Groq API (Llama 3.3-70B) |
| Build Tool | Maven |

---

## OOP Concepts Used

- **MVC Architecture** — Document (Model), EditorPane (View), EditorWindow (Controller)
- **Observer Pattern** — DocumentChangeListener for reactive UI updates
- **Inheritance** — Custom Swing components extending JPanel, JMenuBar, JComponent
- **Encapsulation** — Clean separation of config, theme, file I/O, and AI logic
- **Polymorphism** — Theme-aware rendering across all UI components
- **Multithreading** — SwingWorker for async AI API calls without freezing the UI

---

## Project Structure

```
src/main/java/com/zohaib/quill/
├── Quill.java                 # Entry point & DPI auto-scaling
├── config/
│   └── ConfigManager.java     # App configuration loader/saver
├── editor/
│   ├── Document.java          # Text document model
│   ├── EditorPane.java        # Editor component with line numbers
│   └── EditorWindow.java      # Main window & controller
├── file/
│   └── FileManager.java       # File I/O operations
├── undo/
│   └── UndoManager.java       # Undo/Redo history
├── ui/
│   ├── ThemeManager.java      # Dark/Light color palettes
│   ├── MenuBar.java           # Application menu bar
│   ├── Toolbar.java           # Quick-action toolbar
│   ├── StatusBar.java         # Editor statistics bar
│   └── FindReplaceDialog.java # Search & replace utility
└── ai/
    ├── AIClient.java          # Groq API HTTP client
    └── AIPanel.java           # AI response sidebar
```

---

## Setup & Run

### Prerequisites
- Java 17 or higher
- Maven
- A free Groq API key from [console.groq.com](https://console.groq.com)

### Build
```bash
git clone https://github.com/YOUR_USERNAME/quill.git
cd quill
mvn clean package
```

### Run
```bash
java -jar target/quill.jar
```

### Configure API Key
On first launch, go to **AI → Set API Key…** and paste your Groq API key.

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| New | `Ctrl+N` |
| Open | `Ctrl+O` |
| Save | `Ctrl+S` |
| Undo / Redo | `Ctrl+Z` / `Ctrl+Y` |
| Find & Replace | `Ctrl+F` |
| AI Fix & Rephrase | `Ctrl+Alt+F` |
| AI Summarize | `Ctrl+Alt+S` |
| AI Explain Code | `Ctrl+Alt+E` |
| Toggle Theme | `Ctrl+T` |
| Increase Font | `Ctrl+=` |
| Decrease Font | `Ctrl+-` |

---

## Author

**Zohaib Hassan** 

---

## License

This project is for educational purposes as part of an OOP course.
