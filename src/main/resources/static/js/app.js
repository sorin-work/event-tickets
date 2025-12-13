// Send Spring Security's CSRF token with every htmx request.
document.addEventListener("htmx:configRequest", function (event) {
  var token = document.querySelector("meta[name=_csrf]");
  var header = document.querySelector("meta[name=_csrf_header]");
  if (token && header) {
    event.detail.headers[header.content] = token.content;
  }
});
