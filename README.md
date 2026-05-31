Veltrix 🤖
Veltrix is an AI-powered Android chat assistant that works online and offline — using Gemini API when connected and switching to an on-device ML model automatically when not.
Three specialized modes — Brainstorming, Learning, and Coding — deliver expert-level responses without complex prompts.

✨ Features

🌐 Online Mode — Powered by Gemini API for high-quality AI responses
📴 Offline Mode — Automatically switches to an on-device ML model with zero internet dependency
🔐 Firebase Authentication — Secure user login and session management
🧠 Brainstorming Mode — Generates creative ideas and explores concepts
📚 Learning Mode — Explains topics clearly, like a personal tutor
💻 Coding Mode — Helps debug, write, and review code like an expert developer
⚡ Reactive UI — Built with Jetpack Compose and StateFlow for smooth, real-time state management


🛠️ Tech Stack
LayerTechnologyLanguageKotlinUIJetpack ComposeArchitectureMVVM + StateFlowAuthenticationFirebase AuthOnline AIGemini APIOffline AIOn-Device ML ModelBackend (upcoming)FastAPI

🚀 Getting Started
Prerequisites

Android Studio Hedgehog or newer
Android device / emulator running API 26+
A Firebase project
A Gemini API key

Setup

Clone the repo

bash   git clone https://github.com/saksham-kochhar/Veltrix.git
   cd Veltrix

Add Firebase config
Download your google-services.json from Firebase Console and place it in the /app directory.
Add API keys
Create a local.properties file in the root directory and add:

   GEMINI_API_KEY=your_gemini_api_key_here

Run the app
Open the project in Android Studio and click Run.


📁 Project Structure
app/
├── data/          # Repositories and data sources
├── domain/        # Use cases and models
├── ui/            # Jetpack Compose screens and components
├── viewmodel/     # MVVM ViewModels with StateFlow
└── utils/         # Helper classes and extensions

🔑 Environment Variables
This project uses the following sensitive files that are not committed to the repo:
FilePurposegoogle-services.jsonFirebase configurationlocal.propertiesAPI keys

🗺️ Roadmap

 Online AI via Gemini API
 Offline on-device ML fallback
 Firebase Authentication
 Specialized chat modes
 FastAPI backend for secure key management
 Play Store release


👤 Author
Saksham Kochhar

GitHub: @saksham-kochhar
LinkedIn: linkedin.com/in/saksham-kochhar


📄 License
This project is open source and available under the MIT License.
