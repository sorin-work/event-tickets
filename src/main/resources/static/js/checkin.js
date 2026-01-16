// Optional camera scanning for the check-in page. Uses the browser's
// BarcodeDetector (Chrome, Edge, Android); elsewhere the button stays hidden
// and staff type codes or use a USB scanner, which behaves like a keyboard.
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
