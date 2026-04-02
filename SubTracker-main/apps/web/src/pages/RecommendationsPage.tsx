import { useEffect, useMemo, useState } from "react";
import { categoryApi } from "../api/categoryApi";
import { recommendationApi } from "../api/recommendationApi";
import { useLanguage } from "../i18n/LanguageProvider";
import type { Category } from "../types/category";
import type { Recommendation } from "../types/recommendation";

const RecommendationsPage = () => {
  const { t } = useLanguage();
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [items, setItems] = useState<Recommendation[]>([]);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isRecommendationsLoading, setIsRecommendationsLoading] = useState(false);

  useEffect(() => {
    const loadCategories = async () => {
      setError("");
      setIsLoading(true);

      try {
        const response = await categoryApi.list();
        setCategories(response);
        if (response.length > 0) {
          setSelectedCategory((current) => current || response[0].name);
        }
      } catch (loadError) {
        setCategories([]);
        setSelectedCategory("");
        setError(loadError instanceof Error ? loadError.message : t("common_error"));
      } finally {
        setIsLoading(false);
      }
    };

    void loadCategories();
  }, []);

  useEffect(() => {
    if (!selectedCategory) {
      setItems([]);
      return;
    }

    const loadRecommendations = async () => {
      setError("");
      setIsRecommendationsLoading(true);

      try {
        const response = await recommendationApi.listByCategory(selectedCategory);
        setItems(response);
      } catch (loadError) {
        setItems([]);
        setError(loadError instanceof Error ? loadError.message : t("common_error"));
      } finally {
        setIsRecommendationsLoading(false);
      }
    };

    void loadRecommendations();
  }, [selectedCategory]);

  const visibleItems = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    if (!normalized) {
      return items;
    }

    return items.filter((item) =>
      [item.currentService, item.alternativeService, item.reason, item.category]
        .join(" ")
        .toLowerCase()
        .includes(normalized)
    );
  }, [items, search]);

  return (
    <div className="page">
      <h1 className="page__title">{t("recommendations_title")}</h1>

      {error ? <p className="form-message form-message--error">{error}</p> : null}

      <section className="section">
        <h2 className="section__title">{t("recommendations_categories")}</h2>
        <div className="pill-row">
          {categories.map((category) => (
            <button
              key={category.id}
              type="button"
              className={`pill-button${selectedCategory === category.name ? " pill-button--active" : ""}`}
              onClick={() => setSelectedCategory(category.name)}
            >
              {category.name}
            </button>
          ))}
        </div>
        {!categories.length ? <p>{isLoading ? t("common_loading") : t("recommendations_empty_categories")}</p> : null}
      </section>

      <section className="section">
        <h2 className="section__title">{t("recommendations_alternatives")}</h2>
        <input
          className="input"
          placeholder={t("recommendations_search_placeholder")}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        {visibleItems.length ? (
          <div className="stack-list">
            {visibleItems.map((item, index) => (
              <div
                key={`${item.category}-${item.currentService}-${item.alternativeService}-${index}`}
                className="card-row"
              >
                <strong>
                  {item.currentService}
                  {" -> "}
                  {item.alternativeService}
                </strong>
                <span>{t("recommendations_category")}: {item.category}</span>
                <span>{item.reason}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>{isRecommendationsLoading ? t("common_loading") : t("recommendations_empty_items")}</p>
        )}
      </section>
    </div>
  );
};

export default RecommendationsPage;
