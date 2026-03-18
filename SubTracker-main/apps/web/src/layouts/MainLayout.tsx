import type { ReactNode } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { useLanguage } from "../i18n/LanguageProvider";

const iconProps = {
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const
};

const DashboardIcon = () => (
  <svg {...iconProps}>
    <path d="M4 13h7V4H4z" />
    <path d="M13 20h7v-9h-7z" />
    <path d="M13 11h7V4h-7z" />
    <path d="M4 20h7v-5H4z" />
  </svg>
);

const SubscriptionIcon = () => (
  <svg {...iconProps}>
    <path d="M5 7.5A2.5 2.5 0 0 1 7.5 5h9A2.5 2.5 0 0 1 19 7.5v9a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 5 16.5z" />
    <path d="M8 9h8" />
    <path d="M8 13h8" />
    <path d="M8 17h5" />
  </svg>
);

const UpcomingIcon = () => (
  <svg {...iconProps}>
    <path d="M7 3v3" />
    <path d="M17 3v3" />
    <rect x="4" y="5" width="16" height="15" rx="2" />
    <path d="M4 9.5h16" />
    <path d="M8 13h3" />
    <path d="M13 13h3" />
    <path d="M8 17h3" />
  </svg>
);

const ReminderIcon = () => (
  <svg {...iconProps}>
    <path d="M15 17H5l2-2v-3.5A5 5 0 0 1 12 6a5 5 0 0 1 5 5.5V15l2 2h-4" />
    <path d="M10 19a2 2 0 0 0 4 0" />
  </svg>
);

const AnalyticsIcon = () => (
  <svg {...iconProps}>
    <path d="M5 19V9" />
    <path d="M12 19V5" />
    <path d="M19 19v-7" />
  </svg>
);

const ImportIcon = () => (
  <svg {...iconProps}>
    <path d="M12 3v12" />
    <path d="m7 10 5 5 5-5" />
    <path d="M5 21h14" />
  </svg>
);

const SettingsIcon = () => (
  <svg {...iconProps}>
    <circle cx="12" cy="12" r="3.1" />
    <path d="M12 2.75v2.1" />
    <path d="M12 19.15v2.1" />
    <path d="m5.48 5.48 1.48 1.48" />
    <path d="m17.04 17.04 1.48 1.48" />
    <path d="M2.75 12h2.1" />
    <path d="M19.15 12h2.1" />
    <path d="m5.48 18.52 1.48-1.48" />
    <path d="m17.04 6.96 1.48-1.48" />
    <path d="M12 6.3a5.7 5.7 0 1 1 0 11.4 5.7 5.7 0 0 1 0-11.4Z" />
  </svg>
);

type NavItem = {
  to: string;
  label: string;
  icon: ReactNode;
};

const MainLayout = () => {
  const { t } = useLanguage();
  const { user } = useAuth();
  const accountEmail = user?.email ?? "No email";
  const accountLogin = user?.email?.split("@")[0] ?? "Account";
  const accountMark = accountLogin.charAt(0).toUpperCase();

  const navItems: NavItem[] = [
    { to: "/dashboard", label: t("nav_dashboard"), icon: <DashboardIcon /> },
    { to: "/subscriptions", label: t("nav_subscriptions"), icon: <SubscriptionIcon /> },
    { to: "/upcoming", label: t("nav_upcoming"), icon: <UpcomingIcon /> },
    { to: "/reminders", label: t("nav_reminders"), icon: <ReminderIcon /> },
    { to: "/analytics", label: t("nav_analytics"), icon: <AnalyticsIcon /> },
    { to: "/import", label: t("nav_import"), icon: <ImportIcon /> },
    { to: "/settings", label: t("nav_settings"), icon: <SettingsIcon /> }
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <span className="sidebar__brand-mark">{accountMark}</span>
          <div>
            <strong>{accountLogin}</strong>
            <p>{accountEmail}</p>
          </div>
        </div>

        <nav className="sidebar__nav" aria-label="Primary navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `sidebar__link${isActive ? " sidebar__link--active" : ""}`
              }
            >
              <span className="sidebar__icon" aria-hidden="true">
                {item.icon}
              </span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <p className="topbar__eyebrow">SubTrack Workspace</p>
        </header>

        <main className="page-frame">
          <Outlet />
        </main>

        <nav className="bottom-nav" aria-label="Mobile navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `bottom-nav__item${isActive ? " bottom-nav__item--active" : ""}`
              }
            >
              <span className="bottom-nav__icon" aria-hidden="true">
                {item.icon}
              </span>
              <span className="bottom-nav__label">{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  );
};

export default MainLayout;
