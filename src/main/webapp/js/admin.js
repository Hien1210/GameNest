(function () {
    "use strict";

    // Avatar dropdown (Admin header). Pure UI toggle — carries no
    // authorization meaning; server-side AdminAuthorizationFilter is what
    // actually protects /admin/* routes.
    var menu = document.getElementById("adminAvatarMenu");
    var btn = document.getElementById("adminAvatarBtn");
    var dropdown = document.getElementById("adminAvatarDropdown");

    if (!menu || !btn || !dropdown) {
        return;
    }

    function isOpen() {
        return menu.classList.contains("open");
    }

    function openMenu() {
        menu.classList.add("open");
        btn.setAttribute("aria-expanded", "true");
        dropdown.setAttribute("aria-hidden", "false");
    }

    function closeMenu() {
        menu.classList.remove("open");
        btn.setAttribute("aria-expanded", "false");
        dropdown.setAttribute("aria-hidden", "true");
    }

    btn.addEventListener("click", function (event) {
        event.stopPropagation();
        if (isOpen()) {
            closeMenu();
        } else {
            openMenu();
        }
    });

    document.addEventListener("click", function (event) {
        if (isOpen() && !menu.contains(event.target)) {
            closeMenu();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && isOpen()) {
            closeMenu();
            btn.focus();
        }
    });
})();
