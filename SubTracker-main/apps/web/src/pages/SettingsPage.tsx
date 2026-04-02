import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userApi } from "../api/userApi";
import { useAuth } from "../hooks/useAuth";
import { useLanguage } from "../i18n/LanguageProvider";
import type { UserProfile } from "../types/user";

const SettingsPage = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { language, setLanguage, t } = useLanguage();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadProfile = async () => {
      setError("");
      setIsLoading(true);

      try {
        const response = await userApi.getMe();
        setProfile(response);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load profile");
      } finally {
        setIsLoading(false);
      }
    };

    void loadProfile();
  }, []);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="page">
      <h1 className="page__title">{t("settings_title")}</h1>
      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("settings_profile")}</h2>
        <div className="stack-list">
          <div className="card-row">
            <strong>{profile?.email ?? (isLoading ? t("common_loading") : t("common_none"))}</strong>
            <span>ID: {profile?.id ?? "-"}</span>
            <span>
              {t("settings_timezone")}: {profile?.timezone ?? t("common_none")}
            </span>
          </div>
        </div>
      </section>

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
