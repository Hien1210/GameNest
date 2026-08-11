<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GameNest - Cộng đồng Game thủ</title>
    <!-- Google Fonts & Material Symbols -->
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined" rel="stylesheet" />
    <style>
        :root {
            --bg-primary: #0a0a0f;
            --bg-secondary: rgba(18, 18, 29, 0.65);
            --bg-card: rgba(30, 30, 45, 0.4);
            --accent-purple: #8b5cf6;
            --accent-cyan: #06b6d4;
            --text-primary: #f3f4f6;
            --text-secondary: #9ca3af;
            --border-color: rgba(255, 255, 255, 0.08);
            --glass-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
        }

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Be Vietnam Pro', sans-serif;
            background-color: var(--bg-primary);
            color: var(--text-primary);
            overflow-x: hidden;
            line-height: 1.6;
        }

        /* Ambient Background */
        .ambient-bg {
            position: fixed;
            top: 0;
            left: 0;
            width: 100vw;
            height: 100vh;
            z-index: -1;
            overflow: hidden;
            background: radial-gradient(circle at 15% 50%, rgba(139, 92, 246, 0.12), transparent 50%),
                        radial-gradient(circle at 85% 30%, rgba(6, 182, 212, 0.12), transparent 50%);
        }

        /* Navbar */
        .navbar {
            position: fixed;
            top: 0;
            width: 100%;
            padding: 15px 5%;
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: rgba(10, 10, 15, 0.8);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--border-color);
            z-index: 100;
        }

        .logo {
            font-size: 1.8rem;
            font-weight: 800;
            background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            text-decoration: none;
            letter-spacing: 1px;
        }

        .nav-links {
            display: flex;
            gap: 35px;
        }

        .nav-links a {
            color: var(--text-secondary);
            text-decoration: none;
            font-weight: 500;
            transition: color 0.3s ease;
            font-size: 1.05rem;
        }

        .nav-links a:hover {
            color: var(--text-primary);
        }

        .nav-auth {
            display: flex;
            gap: 15px;
            align-items: center;
        }

        .btn-login {
            color: var(--text-primary);
            text-decoration: none;
            font-weight: 500;
            padding: 8px 16px;
            border-radius: 8px;
            transition: background 0.3s ease;
        }
        
        .btn-login:hover {
            background: rgba(255, 255, 255, 0.05);
        }

        .btn-register {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan));
            color: white;
            text-decoration: none;
            padding: 8px 20px;
            border-radius: 8px;
            font-weight: 600;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
            box-shadow: 0 4px 15px rgba(139, 92, 246, 0.3);
        }

        .btn-register:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(139, 92, 246, 0.5);
        }

        /* ===== HERO SECTION ===== */
        .hero {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
            padding: 0 20px;
            position: relative;
            overflow: hidden;
        }

        /* --- Marquee Background Layer --- */
        .marquee-bg {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 0;
            display: flex;
            flex-direction: column;
            justify-content: center;
            gap: 16px;
            padding: 60px 0;
            opacity: 0.35;
        }

        .marquee-row {
            display: flex;
            width: max-content;
            gap: 16px;
        }

        .marquee-row .marquee-track {
            display: flex;
            gap: 16px;
            animation-timing-function: linear;
            animation-iteration-count: infinite;
            will-change: transform;
        }

        /* Row 1: Right to Left */
        .marquee-row--1 .marquee-track {
            animation-name: marqueeScrollLeft;
            animation-duration: 25s;
        }

        /* Row 2: Left to Right */
        .marquee-row--2 .marquee-track {
            animation-name: marqueeScrollRight;
            animation-duration: 35s;
        }

        /* Row 3: Right to Left */
        .marquee-row--3 .marquee-track {
            animation-name: marqueeScrollLeft;
            animation-duration: 30s;
        }

        @keyframes marqueeScrollLeft {
            0% { transform: translateX(0); }
            100% { transform: translateX(-50%); }
        }

        @keyframes marqueeScrollRight {
            0% { transform: translateX(-50%); }
            100% { transform: translateX(0); }
        }

        .game-cover {
            width: 220px;
            height: 130px;
            border-radius: 12px;
            overflow: hidden;
            flex-shrink: 0;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
            border: 1px solid rgba(255, 255, 255, 0.06);
        }

        .game-cover img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
        }

        /* --- Dark Overlay Layer --- */
        .hero-overlay {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 1;
            pointer-events: none;
            /* Radial: bright center, dark edges. Vertical: dark top/bottom */
            background:
                radial-gradient(ellipse 60% 50% at 50% 55%, rgba(10,10,15,0.3) 0%, rgba(10,10,15,0.85) 100%),
                linear-gradient(to bottom, rgba(10,10,15,0.9) 0%, rgba(10,10,15,0.2) 25%, rgba(10,10,15,0.2) 65%, rgba(10,10,15,1) 100%);
        }

        /* Horizontal gradient masks on the left and right edges */
        .hero-edge-fade-left,
        .hero-edge-fade-right {
            position: absolute;
            top: 0;
            width: 15%;
            height: 100%;
            z-index: 1;
            pointer-events: none;
        }
        .hero-edge-fade-left {
            left: 0;
            background: linear-gradient(to right, var(--bg-primary) 0%, transparent 100%);
        }
        .hero-edge-fade-right {
            right: 0;
            background: linear-gradient(to left, var(--bg-primary) 0%, transparent 100%);
        }

        /* --- Hero Content Layer --- */
        .hero-content {
            position: relative;
            z-index: 2;
            display: flex;
            flex-direction: column;
            align-items: center;
            width: 100%;
        }

        .hero h1 {
            font-size: 4.5rem;
            font-weight: 800;
            margin-bottom: 20px;
            line-height: 1.1;
            background: linear-gradient(to right, #ffffff, var(--text-secondary));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            animation: slideUp 0.8s ease-out;
        }

        .hero h1 span {
            background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .hero p {
            font-size: 1.25rem;
            color: var(--text-secondary);
            max-width: 600px;
            margin-bottom: 40px;
            animation: slideUp 1s ease-out;
        }

        .hero-btns {
            display: flex;
            gap: 20px;
            animation: slideUp 1.2s ease-out;
        }

        .btn-primary {
            background: linear-gradient(135deg, var(--accent-purple), var(--accent-cyan));
            color: white;
            text-decoration: none;
            padding: 16px 40px;
            border-radius: 30px;
            font-weight: 600;
            font-size: 1.1rem;
            transition: all 0.3s ease;
            box-shadow: 0 4px 20px rgba(139, 92, 246, 0.4);
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .btn-primary:hover {
            transform: translateY(-3px) scale(1.02);
            box-shadow: 0 8px 30px rgba(139, 92, 246, 0.6);
        }

        .btn-secondary {
            background: rgba(255, 255, 255, 0.05);
            color: var(--text-primary);
            text-decoration: none;
            padding: 16px 40px;
            border-radius: 30px;
            font-weight: 600;
            font-size: 1.1rem;
            border: 1px solid var(--border-color);
            transition: all 0.3s ease;
            backdrop-filter: blur(10px);
        }

        .btn-secondary:hover {
            background: rgba(255, 255, 255, 0.1);
            border-color: var(--text-secondary);
        }

        /* ===== FEATURES SECTION ===== */
        .features {
            padding: 100px 5%;
            max-width: 1200px;
            margin: 0 auto;
        }

        .section-title {
            text-align: center;
            font-size: 2.5rem;
            font-weight: 700;
            margin-bottom: 60px;
        }

        .features-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 30px;
        }

        .feature-card {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 20px;
            padding: 40px 30px;
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            transition: all 0.4s ease;
            position: relative;
            overflow: hidden;
        }

        .feature-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: linear-gradient(135deg, rgba(139,92,246,0.1), rgba(6,182,212,0.1));
            opacity: 0;
            transition: opacity 0.4s ease;
            z-index: 0;
        }

        .feature-card:hover {
            transform: translateY(-10px);
            border-color: rgba(139, 92, 246, 0.3);
            box-shadow: 0 15px 40px rgba(0, 0, 0, 0.4);
        }

        .feature-card:hover::before {
            opacity: 1;
        }

        .feature-icon {
            font-size: 44px;
            color: var(--accent-cyan);
            margin-bottom: 20px;
            position: relative;
            z-index: 1;
        }

        .feature-card:nth-child(2) .feature-icon { color: var(--accent-purple); }
        .feature-card:nth-child(3) .feature-icon { color: #ec4899; }

        .feature-card h3 {
            font-size: 1.5rem;
            margin-bottom: 15px;
            font-weight: 600;
            position: relative;
            z-index: 1;
        }

        .feature-card p {
            color: var(--text-secondary);
            font-size: 1rem;
            line-height: 1.6;
            position: relative;
            z-index: 1;
        }

        /* ===== ANIMATIONS ===== */
        @keyframes slideUp {
            from { opacity: 0; transform: translateY(40px); }
            to { opacity: 1; transform: translateY(0); }
        }

        /* ===== ACCESSIBILITY ===== */
        @media (prefers-reduced-motion: reduce) {
            .marquee-row .marquee-track {
                animation-play-state: paused !important;
            }
        }

        /* ===== RESPONSIVE ===== */
        @media (max-width: 1024px) {
            .game-cover {
                width: 180px;
                height: 106px;
            }
            .marquee-bg {
                gap: 12px;
            }
        }

        @media (max-width: 768px) {
            .hero h1 { font-size: 3.2rem; }
            .nav-links { display: none; }
            .hero-btns { flex-direction: column; width: 100%; max-width: 300px; }
            .btn-primary, .btn-secondary { width: 100%; justify-content: center; }
            .navbar { padding: 15px 20px; }

            .game-cover {
                width: 140px;
                height: 82px;
                border-radius: 8px;
            }
            .marquee-bg {
                gap: 10px;
            }
            /* Hide the third row on small screens */
            .marquee-row--3 {
                display: none;
            }
        }

        @media (max-width: 480px) {
            .hero h1 { font-size: 2.5rem; }
            .game-cover {
                width: 120px;
                height: 70px;
            }
        }
    </style>
</head>
<body>

    <div class="ambient-bg"></div>

    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/" class="logo">GameNest</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/games">Games</a>
            <a href="${pageContext.request.contextPath}/questions">Hỏi Đáp</a>
            <a href="#">Tìm Đội (LFG)</a>
            <a href="#">Cộng Đồng</a>
        </div>
        <div class="nav-auth">
            <a href="${pageContext.request.contextPath}/login" class="btn-login">Đăng nhập</a>
            <a href="${pageContext.request.contextPath}/register" class="btn-register">Đăng ký</a>
        </div>
    </nav>

    <section class="hero">
        <%-- Layer 1: Multi-row Marquee Background --%>
        <div class="marquee-bg" id="marqueeBg">
            <%-- JS will build 3 rows here --%>
        </div>

        <%-- Layer 2: Dark / Gradient Overlay --%>
        <div class="hero-overlay"></div>
        <div class="hero-edge-fade-left"></div>
        <div class="hero-edge-fade-right"></div>

        <%-- Layer 3: Hero Content --%>
        <div class="hero-content">
            <h1>Kết Nối. Chinh Phục.<br><span>Tỏa Sáng.</span></h1>
            <p>Tham gia GameNest ngay hôm nay. Nơi bạn có thể khám phá những tựa game đỉnh cao, tìm kiếm đồng đội tâm giao và xây dựng đội ngũ vô địch của riêng mình.</p>
            <div class="hero-btns">
                <a href="${pageContext.request.contextPath}/register" class="btn-primary">
                    Tham gia ngay <span class="material-symbols-outlined">rocket_launch</span>
                </a>
                <a href="${pageContext.request.contextPath}/games" class="btn-secondary">Khám phá Games</a>
            </div>
        </div>
    </section>

    <section class="features">
        <h2 class="section-title">Khám Phá Tính Năng</h2>
        <div class="features-grid">
            <div class="feature-card">
                <span class="material-symbols-outlined feature-icon">sports_esports</span>
                <h3>Thư Viện Game</h3>
                <p>Tra cứu thông tin hàng ngàn tựa game phong phú. Đánh giá, thảo luận và luôn cập nhật những tin tức, xu hướng mới nhất từ thị trường game.</p>
            </div>
            <div class="feature-card">
                <span class="material-symbols-outlined feature-icon">forum</span>
                <h3>Hỏi Đáp & Thảo Luận</h3>
                <p>Mắc kẹt ở một màn khó? Đặt câu hỏi và nhận những lời khuyên, thủ thuật chất lượng từ hàng ngàn game thủ kỳ cựu trong cộng đồng.</p>
            </div>
            <div class="feature-card">
                <span class="material-symbols-outlined feature-icon">groups</span>
                <h3>Tìm Bạn Chơi (LFG)</h3>
                <p>Không còn phải chịu cảnh leo rank đơn độc. Đăng tin tìm đồng đội (Looking For Group) và xây dựng ngay một đội hình hoàn hảo cho mọi trận chiến.</p>
            </div>
        </div>
    </section>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            var ctx = "${pageContext.request.contextPath}";

            // 3 hàng game cover — dễ dàng thay đổi / thêm bớt tại đây
            var rows = [
                // Row 1: ← ← ← (Right to Left)
                [
                    ctx + "/images/Cs2.png",
                    ctx + "/images/Valorant.png",
                    ctx + "/images/Minecraft.png",
                    ctx + "/images/GTA5.png",
                    ctx + "/images/FcOnline.png",
                    ctx + "/images/PUBG.png",
                    ctx + "/images/ApexLegends.png"
                ],
                // Row 2: → → → (Left to Right)
                [
                    ctx + "/images/Fortnite.png",
                    ctx + "/images/DeltaForce.png",
                    ctx + "/images/Battlefield2042.png",
                    ctx + "/images/CallOfDutyWarzone.png",
                    ctx + "/images/ZenlessZoneZero.png",
                    ctx + "/images/Efootball.png",
                    ctx + "/images/ArenaBreakout.png"
                ],
                // Row 3: ← ← ← (Right to Left)
                [
                    ctx + "/images/BlackMythWukong.png",
                    ctx + "/images/ForzaHorizon6.png",
                    ctx + "/images/Battlefield6.png",
                    ctx + "/images/ReadyOrNot.png",
                    ctx + "/images/Bloodstrike.png",
                    ctx + "/images/FcMobile.png",
                    ctx + "/images/Roblox.png"
                ]
            ];

            var container = document.getElementById('marqueeBg');

            rows.forEach(function(imgList, rowIndex) {
                // Tạo wrapper cho hàng
                var rowDiv = document.createElement('div');
                rowDiv.className = 'marquee-row marquee-row--' + (rowIndex + 1);

                // Tạo track chứa ảnh (sẽ duplicate để loop seamless)
                var track = document.createElement('div');
                track.className = 'marquee-track';

                // Render ảnh gốc + duplicate (x2) để CSS animation loop liền mạch
                for (var copy = 0; copy < 2; copy++) {
                    for (var i = 0; i < imgList.length; i++) {
                        var cover = document.createElement('div');
                        cover.className = 'game-cover';
                        var img = document.createElement('img');
                        img.src = imgList[i];
                        img.alt = 'Game Cover';
                        img.loading = (copy === 0 && i < 3) ? 'eager' : 'lazy';
                        cover.appendChild(img);
                        track.appendChild(cover);
                    }
                }

                rowDiv.appendChild(track);
                container.appendChild(rowDiv);
            });
        });
    </script>
</body>
</html>
