import { Link } from "react-router-dom";
import { useLanguage } from "../i18n/LanguageProvider";

const LandingPage = () => {
  const { language, setLanguage, t } = useLanguage();

  return (
    <div className="auth-shell">
      <div className="auth-shell__backdrop" />
      <div className="landing-card">
        <header className="landing-card__top">
          <div>
            <p className="landing-brand" aria-label="SubTrack">
              <span className="landing-brand__text">
                <span className="landing-brand__main">Sub</span>
                <span className="landing-brand__script">Track</span>
              </span>
            </p>
            <p className="landing-card__eyebrow">{t("landing_showcase_kicker")}</p>
          </div>

          <div className="landing-card__controls">
            <div className="landing-card__lang">
              <span>{t("settings_language")}</span>
              <div className="pill-row">
                <button
                  type="button"
                  className={`pill-button${language === "RU" ? " pill-button--active" : ""}`}
                  onClick={() => setLanguage("RU")}
                >
                  {t("labels_russian")}
                </button>
                <button
                  type="button"
                  className={`pill-button${language === "EN" ? " pill-button--active" : ""}`}
                  onClick={() => setLanguage("EN")}
                >
                  {t("labels_english")}
                </button>
              </div>
            </div>

            <div className="landing-card__actions landing-card__actions--top">
              <Link to="/login" className="button">
                {t("auth_login_button")}
              </Link>
              <Link to="/register" className="button button--secondary">
                {t("auth_register_button")}
              </Link>
            </div>
          </div>
        </header>

        <div className="landing-card__body">
          <section className="landing-card__showcase">
            <h1>{t("landing_showcase_title")}</h1>
            <p>{t("landing_showcase_subtitle")}</p>
          </section>

          <section className="landing-card__panel">
            <h2>{t("landing_panel_title")}</h2>
            <p className="landing-card__subtitle">{t("landing_panel_subtitle")}</p>

            <div className="landing-card__grid">
              <div>
                <strong>{t("landing_feature_analytics_title")}</strong>
                <span>{t("landing_feature_analytics_text")}</span>
              </div>
              <div>
                <strong>{t("landing_feature_import_title")}</strong>
                <span>{t("landing_feature_import_text")}</span>
              </div>
              <div>
                <strong>{t("landing_feature_upcoming_title")}</strong>
                <span>{t("landing_feature_upcoming_text")}</span>
              </div>
            </div>
          </section>

          <div className="landing-card__actions landing-card__actions--mobile">
            <Link to="/login" className="button">
              {t("auth_login_button")}
            </Link>
            <Link to="/register" className="button button--secondary">
              {t("auth_register_button")}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LandingPage;
