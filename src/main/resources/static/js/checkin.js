// Optional camera scanning for the check-in page. Uses the browser's
// BarcodeDetector (Chrome, Edge, Android); elsewhere the button stays hidden
// and staff type codes or use a USB scanner, which behaves like a keyboard.
(function () {
  // Clear the field after every scan so the next code can be typed or
  // scanned straight away. (Done here rather than with hx-on, which the
  // content security policy does not allow.)
  var scanForm = document.querySelector("[data-scan-form]");
  if (scanForm) {
    scanForm.addEventListener("htmx:afterRequest", function (event) {
      if (event.detail.successful) {
        scanForm.reset();
        scanForm.code.focus();
      }
    });
  }
})();

(function () {
  var button = document.querySelector("[data-scan-camera]");
  var video = document.querySelector("[data-scan-video]");
  var form = document.querySelector("[data-scan-form]");
  if (!button || !video || !form || !("BarcodeDetector" in window)) return;

  var detector = new BarcodeDetector({ formats: ["qr_code"] });
  var stream = null;
  var lastCode = null;
  var lastAt = 0;
  button.hidden = false;

  function stop() {
    if (stream) stream.getTracks().forEach(function (t) { t.stop(); });
    stream = null;
    video.hidden = true;
    button.textContent = "Scan with camera";
  }

  function submit(code) {
    // The same QR code stays in view for a while; ignore repeats for 3s.
    var now = Date.now();
    if (code === lastCode && now - lastAt < 3000) return;
    lastCode = code;
    lastAt = now;
    form.code.value = code;
    if (window.htmx) {
      htmx.trigger(form, "submit");
    } else {
      form.submit();
    }
  }

  function scan() {
    if (!stream) return;
    detector
      .detect(video)
      .then(function (codes) {
        if (codes.length > 0) submit(codes[0].rawValue);
      })
      .catch(function () {})
      .finally(function () {
        if (stream) requestAnimationFrame(scan);
      });
  }

  button.addEventListener("click", function () {
    if (stream) return stop();
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: "environment" } })
      .then(function (s) {
        stream = s;
        video.srcObject = s;
        video.hidden = false;
        video.play();
        button.textContent = "Stop camera";
        scan();
      })
      .catch(function () {
        button.textContent = "Camera not available";
        button.disabled = true;
      });
  });
})();

// A short beep after each scan: high for "let in", low for "not admitted",
// so door staff do not have to look at the screen every time.
(function () {
  var AudioContext = window.AudioContext || window.webkitAudioContext;
  if (!AudioContext) return;
  var audio = null;

  function beep(frequency, duration) {
    audio = audio || new AudioContext();
    var oscillator = audio.createOscillator();
    var gain = audio.createGain();
    oscillator.frequency.value = frequency;
    gain.gain.value = 0.15;
    oscillator.connect(gain);
    gain.connect(audio.destination);
    oscillator.start();
    oscillator.stop(audio.currentTime + duration);
  }

  document.body.addEventListener("htmx:afterSettle", function (event) {
    if (event.detail.elt.id !== "check-in-result") return;
    var box = document.querySelector("#check-in-result .check-in-result");
    if (!box) return;
    if (box.classList.contains("alert-ok")) {
      beep(880, 0.12);
    } else {
      beep(220, 0.35);
    }
  });
})();
