# FILETRANS - Java 21 (runtime) instructions

This project was compiled targeting Java 21. You can run it with any JDK >= 21 (including JDK 23) using the provided scripts.

Prerequisites
- JDK 21 or newer installed (JDK 23 works). Ensure `java` and `javac` are on PATH or set `JAVA_HOME` appropriately.

Build
1. Compile all sources (example using PowerShell):

```powershell
Set-Location 'C:\Users\aadis\OneDrive\Documents\FILETRANS'
$files = Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac --release 21 -cp 'libs\mysql-connector-j-8.0.33.jar' -d out $files
```

Run server (GUI)
```powershell
# From project root
java -cp "out;libs\mysql-connector-j-8.0.33.jar" server.ServerApp
```

Run server (headless, serve a specific file)
```powershell
# Using helper script
scripts\start-server.ps1 -FilePath C:\path\to\file.txt
```

Run client (GUI)
```powershell
java -cp "out;libs\mysql-connector-j-8.0.33.jar" client.ClientApp
```

Run headless test client
```powershell
scripts\start-client.ps1 -Server 127.0.0.1 -FileName serve_test.txt
```

Notes
- Database logging (MySQL) is optional; if DB is not available the server will continue to serve files but log stack traces to stdout.
- The transport is plaintext TCP; file contents are encrypted with AES before sending but you should add TLS and authentication for production use.Upgrade to Java 21 (LTS) - Project notes

Goal
- Ensure the project is compiled and run targeting Java 21 (LTS). Some users may have newer JDKs (e.g., JDK 23). This repo contains helper scripts that compile with --release 21 so you can use a newer JDK to produce Java 21-compatible classes.

Options
1) Install JDK 21 (recommended for strict LTS parity)
   - Download and install a Java 21 JDK (Adoptium Temurin, Oracle, or other vendors).
   - Set JAVA_HOME to the JDK 21 install directory and add %JAVA_HOME%\bin to PATH.
     Example (PowerShell):

     $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
     $env:Path = "$env:JAVA_HOME\bin;" + $env:Path

2) Use your existing JDK (e.g., JDK 23) and compile for Java 21
   - JDKs >= 9 support the --release flag which ensures the produced classes are compatible with the specified Java version.
   - This repo includes PowerShell scripts to compile and run using --release 21.

Quick steps (PowerShell)

# From the repo root
# 1) Compile targeting Java 21
.\scripts\compile.ps1

# 2) Run the server
.\scripts\run-server.ps1

Notes
- The compile script will look for java compiler in %JAVA_HOME% (if set) or fall back to the javac/jave on PATH.
- The run script composes the classpath with the compiled classes (./bin) and libraries under ./libs (e.g., MySQL connector).
- If you need to actually require a Java 21 runtime for production, install a Java 21 JDK and set JAVA_HOME accordingly. Running the application on a newer JVM (like JDK 23) will generally work, but runtime differences can exist.

Troubleshooting
- If compilation fails due to APIs not present in Java 21, inspect error messages and update code accordingly.
- If the GUI does not start, ensure AWT/Swing support is available in your environment (headless servers require X11 or headful environment).

Contact
- If you want, I can try to compile the project here or adjust build scripts for Maven/Gradle if you prefer a proper build tool.
