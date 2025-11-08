# 👁️ Watcher - Detector Inteligente de Mirada

Watcher es una aplicación de Android diseñada para proteger tu privacidad mediante el uso de la cámara frontal para detectar si estás prestando atención a la pantalla. Si la aplicación detecta que has dejado de mirar, puede realizar acciones automáticas, como bloquear la pantalla, para asegurar que nadie más vea tu contenido.

Esta aplicación utiliza tecnologías modernas de Android como Jetpack Compose para la interfaz de usuario, CameraX para el acceso a la cámara y ML Kit de Google para la detección de rostros y análisis de la mirada en tiempo real.

## 🚀 Características Principales

* **Detección de Mirada en Tiempo Real:** Utiliza la cámara frontal y ML Kit para analizar la orientación de la cabeza y si los ojos están abiertos.
* **Bloqueo Automático de Pantalla:** Si el usuario no está mirando, la app puede mostrar un *overlay* que bloquea la vista o interactuar con el sistema para apagar la pantalla.
* **Calibración Personalizable:** Permite al usuario ajustar la sensibilidad de la detección para adaptarla a su entorno y forma de usar el dispositivo.
    * Sensibilidad de "ojos abiertos".
    * Umbral de rotación de cabeza (horizontal y vertical).
* **Servicio en Segundo Plano:** La detección puede seguir funcionando discretamente en segundo plano mientras usas otras aplicaciones.
* **Interfaz de Usuario Moderna:** Construida 100% con Jetpack Compose y Material Design 3.
* **Gestión de Permisos Clara:** Guía al usuario para otorgar los permisos necesarios (cámara, superposición de pantalla, notificaciones).
* **Privacidad Primero:** Todo el procesamiento de imágenes se realiza **localmente en el dispositivo**. Ninguna imagen o dato de la cámara sale de tu teléfono.

## 🛠️ Tecnologías y Librerías Utilizadas

Este proyecto es una demostración de un stack de desarrollo moderno para Android:

* **Lenguaje:** Kotlin (100% Kotlin)
* **UI:** Jetpack Compose
* **Arquitectura:** MVVM (Model-View-ViewModel) con ViewModel de Jetpack.
* **Navegación:** Navigation Compose
* **Cámara:** CameraX para un acceso a la cámara robusto y simplificado.
* **Machine Learning:** ML Kit Face Detection de Google para el análisis de rostros.
* **Gestión de Dependencias:** Gradle Version Catalogs para una gestión centralizada y limpia.
* **Servicios en Segundo Plano:** ForegroundService para mantener la detección activa.

## 📋 Requisitos Previos

* Android Studio Iguana | 2023.2.1 o superior.
* Dispositivo o emulador con Android 10 (API 29) o superior.
* Un dispositivo con cámara frontal para probar la funcionalidad principal.

## ⚙️ Instalación y Puesta en Marcha

Sigue estos pasos para compilar y ejecutar el proyecto en tu máquina local:

1.  **Clona el repositorio:**
    *(Reemplaza `tu-usuario` con tu nombre de usuario de GitHub)*
    ```bash
    git clone [https://github.com/tu-usuario/watcher.git](https://github.com/tu-usuario/watcher.git)
    ```

2.  **Configura Firebase (Opcional, pero recomendado):**
    * Ve a la [Consola de Firebase](https://console.firebase.google.com/).
    * Crea un nuevo proyecto.
    * Registra una nueva aplicación de Android con el nombre de paquete: `com.jhoxmanv.watcher`.
    * Descarga el archivo `google-services.json`.
    * Coloca el archivo `google-services.json` en el directorio `app/` de tu proyecto.

3.  **Abre el proyecto en Android Studio:**
    * Selecciona "Open an existing project" y elige la carpeta que clonaste.

4.  **Sincroniza y Compila:**
    * Gradle se sincronizará automáticamente. Si no, haz clic en "Sync Project with Gradle Files".
    * Construye el proyecto con `Build > Make Project`.

5.  **Ejecuta la aplicación:**
    * Selecciona un dispositivo o emulador y haz clic en el botón "Run".

## 📖 Uso de la Aplicación

1.  Al abrir la app por primera vez, se te guiará para otorgar los permisos necesarios:
    * **Cámara:** Para la detección de rostros.
    * **Superponer sobre otras apps:** Para mostrar el *overlay* de bloqueo.
    * **Notificaciones:** Para mostrar la notificación persistente del servicio en segundo plano.
2.  Una vez otorgados los permisos, llegarás a la pantalla principal donde podrás iniciar o detener el servicio de detección.
3.  Desde la pantalla principal, puedes acceder a **Ajustes** para configurar:
    * **Tiempo de bloqueo:** Cuántos segundos esperar antes de bloquear la pantalla.
    * **Calibración de la Mirada:** Accede a una pantalla con vista previa de la cámara para ajustar la sensibilidad de la detección a tu gusto.

## 🤝 Contribuciones

Las contribuciones son siempre bienvenidas. Si quieres mejorar la aplicación, por favor sigue estos pasos:

1.  Haz un *Fork* del repositorio.
2.  Crea una nueva rama para tu funcionalidad (`git checkout -b feature/AmazingFeature`).
3.  Haz tus cambios y realiza *commits* (`git commit -m 'Add some AmazingFeature'`).
4.  Haz *Push* a tu rama (`git push origin feature/AmazingFeature`).
5.  Abre un *Pull Request*.

## 📄 Licencia

Este proyecto está bajo la Licencia **Creative Commons Atribución-NoComercial-CompartirIgual 4.0 Internacional (CC BY-NC-SA 4.0)**.

Esto significa que eres libre de usar, compartir y modificar este trabajo, siempre y cuando:
1.  Me des el crédito apropiado.
2.  **No lo uses para fines comerciales.**
3.  Si lo modificas, lo compartas bajo esta misma licencia.

Consulta el archivo `LICENSE.md` para más detalles.
---

Creado con ❤️ por JhoxmanV - github.com/JhoxmanXD
