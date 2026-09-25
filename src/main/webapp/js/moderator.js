(function () {
    "use strict";

    // Avatar dropdown (Moderator header). Pure UI toggle — carries no
    // authorization meaning; server-side ModeratorAuthorizationFilter is
    // what actually protects /moderator/* routes.
    var menu = document.getElementById("modAvatarMenu");
    var btn = document.getElementById("modAvatarBtn");
    var dropdown = document.getElementById("modAvatarDropdown");

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
