// Counts down to the end of a checkout hold. Markup:
//   <span data-countdown="2030-01-15T10:10:00Z">10:00</span>
// When the time runs out the surrounding form's submit button is disabled
// and the element with [data-countdown-expired] is shown.
(function () {
  function pad(n) {
    return n < 10 ? "0" + n : String(n);
  }

  function start(el) {
    var deadline = Date.parse(el.getAttribute("data-countdown"));
    if (isNaN(deadline)) return;
    var form = el.closest("form");
    var expired = document.querySelector("[data-countdown-expired]");

    function tick() {
      var left = Math.max(0, Math.round((deadline - Date.now()) / 1000));
      el.textContent = Math.floor(left / 60) + ":" + pad(left % 60);
      if (left === 0) {
        clearInterval(timer);
        if (form) {
          form.querySelectorAll("button[type=submit]").forEach(function (b) {
            b.disabled = true;
          });
        }
        if (expired) expired.hidden = false;
      }
    }

    var timer = setInterval(tick, 1000);
    tick();
  }

  document.querySelectorAll("[data-countdown]").forEach(start);
})();
