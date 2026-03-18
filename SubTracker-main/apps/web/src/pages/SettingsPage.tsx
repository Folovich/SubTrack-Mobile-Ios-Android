import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { useLanguage } from "../i18n/LanguageProvider";

const SettingsPage = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { language, setLanguage, t } = useLanguage();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="page">
      <h1 className="page__title">{t("settings_title")}</h1>
      <section className="section">
        <h2 className="section__title">{t("settings_language")}</h2>
        <div className="pill-row">
          <button
            type="button"
            className={`pill-button${language === "EN" ? " pill-button--active" : ""}`}
            onClick={() => setLanguage("EN")}
          >
            {t("labels_english")}
          </button>
          <button
            type="button"
            className={`pill-button${language === "RU" ? " pill-button--active" : ""}`}
            onClick={() => setLanguage("RU")}
          >
            {t("labels_russian")}
          </button>
        </div>
        <p className="muted">
          {t("settings_selected")}: {language}
        </p>
      </section>
      <button type="button" className="text-action text-action--center" onClick={handleLogout}>
        {t("settings_logout")}
      </button>
    </div>
  );
};

export default SettingsPage;
