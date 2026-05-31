// Send Spring Security's CSRF token with every htmx request.
document.addEventListener("htmx:configRequest", function (event) {
  var token = document.querySelector("meta[name=_csrf]");
  var header = document.querySelector("meta[name=_csrf_header]");
  if (token && header) {
    event.detail.headers[header.content] = token.content;
  }
});

// Buttons with data-copy="text" copy the text to the clipboard.
document.addEventListener("click", function (event) {
  var button = event.target.closest("[data-copy]");
  if (!button || !navigator.clipboard) return;
  navigator.clipboard.writeText(button.getAttribute("data-copy")).then(function () {
    var label = button.textContent;
    button.textContent = "Copied";
    setTimeout(function () {
      button.textContent = label;
    }, 1500);
  });
});
