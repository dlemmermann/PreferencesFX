package com.dlsc.preferencesfx;

import static com.dlsc.preferencesfx.util.StringUtils.containsIgnoreCase;

import com.dlsc.preferencesfx.util.PreferencesFxUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;
import org.eclipse.fx.ui.controls.tree.TreeItemPredicate;

public class CategoryTree extends TreeView {

  private static final Logger LOGGER =
      LogManager.getLogger(CategoryTree.class.getName());

  PreferencesFx preferencesFx;

  List<Category> categoriesLst;
  List<Setting> settingsLst;
  List<Category> filteredCategoriesLst;
  List<Setting> filteredSettingsLst;

  private HashMap<Category, FilterableTreeItem<Category>> categoryTreeItemMap = new HashMap<>();
  private HashMap<Setting, Category> settingCategoryMap;

  private List<Category> categories;
  private FilterableTreeItem<Category> rootItem;
  private StringProperty searchText = new SimpleStringProperty();
  private Predicate<Category> filterPredicate = category -> {
    // look in category description for matches
    boolean categoryMatch = containsIgnoreCase(category.getDescription(), searchText.get());
    boolean settingMatch = false;
    if (category.getGroups() != null) {
      // look in settings too
      settingMatch = category.getGroups().stream()
          .map(Group::getSettings)      // get settings from groups
          .flatMap(Collection::stream)  // flatten all lists of settings to settings
          .anyMatch(setting -> containsIgnoreCase(setting.getDescription(), searchText.get()));
    }
    return categoryMatch || settingMatch;
  };

  public CategoryTree(PreferencesFx preferencesFx, List<Category> categories) {
    this.categories = categories;
    this.preferencesFx = preferencesFx;
    setupParts();
    layoutParts();
    setupBindings();
    addListeners();
  }

  private void addListeners() {
    // Update category upon selection
    getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (newValue != null) {
            preferencesFx.showCategory((Category) ((TreeItem) newValue).getValue());
          }
        }
    );

    // Filter TreeView upon Search
    searchText.addListener((observable, oldText, newText) -> {
      filteredCategoriesLst = PreferencesFxUtils.filterCategoriesByDescription(categoriesLst, newText);
      filteredSettingsLst = PreferencesFxUtils.filterSettingsByDescription(settingsLst, newText);
      int amountCategories = filteredCategoriesLst.size();
      int amountSettings = filteredSettingsLst.size();
      LOGGER.trace("Matched Categories: " + amountCategories);
      LOGGER.trace("Matched Settings: " + amountSettings);
      // if there is one category left, select it
      if (amountCategories == 1) {
        setSelectedItem(filteredCategoriesLst.get(0));
      }
      if (amountSettings == 1) {
        setSelectedItem(settingCategoryMap.get(filteredSettingsLst.get(0)));
      }
      // Remove all markings from settings
      Category selectedCategory = preferencesFx.getDisplayedCategory();
      if (selectedCategory != null) {
        selectedCategory.unmarkSettings();
      }
      if (amountSettings >= 1) {
        filteredSettingsLst.stream().forEach(Setting::mark);
      }
    });
  }

  private void setupParts() {
    rootItem = new FilterableTreeItem<>(Category.of("Root"));
    addRecursive(rootItem, categories);
    categoriesLst = new ArrayList<>(categoryTreeItemMap.keySet());
    PreferencesFxUtils.mapSettingsToCategories(categoriesLst);
    settingsLst = PreferencesFxUtils.categoriesToSettings(categoriesLst);
  }

  private void addRecursive(FilterableTreeItem treeItem, List<Category> categories) {
    for (Category category : categories) {
      FilterableTreeItem<Category> item = new FilterableTreeItem<>(category);
      // If there are subcategries, add them recursively.
      if (!Objects.equals(category.getChildren(), null)) {
        addRecursive(item, category.getChildren());
      }
      treeItem.getInternalChildren().add(item);
      categoryTreeItemMap.put(category, item);
    }
  }

  private void layoutParts() {
    setRoot(rootItem);
    // TreeView requires a RootItem, but in this case it's not desired to have it visible.
    setShowRoot(false);
    getRoot().setExpanded(true);
    // Set initial selected category.
    getSelectionModel().select(PreferencesFx.DEFAULT_CATEGORY);
  }

  private void setupBindings() {
    // Make TreeView filterable by implementing the necessary binding
    rootItem.predicateProperty().bind(Bindings.createObjectBinding(() -> {
      if (searchText.get() == null || searchText.get().isEmpty()) {
        return null;
      }
      return TreeItemPredicate.create(filterPredicate);
    }, searchText));
  }

  public StringProperty searchTextProperty() {
    return searchText;
  }

  /**
   * Sets the selected item in the TreeView to the category of the given categoryId.
   *
   * @param categoryId the id of the category to be found
   * @return the category with categoryId or the first category in the TreeView if none is found
   */
  public void setSelectedCategoryById(int categoryId) {
    Category category = findCategoryById(categoryId);
    setSelectedItem(category);
  }

  /**
   * Finds the category with the matching id.
   *
   * @param categoryId the id of the category to be found
   * @return the category with categoryId or the first category in the TreeView if none is found
   */
  public Category findCategoryById(int categoryId) {
    Category selectedCategory = categoryTreeItemMap.keySet().stream().filter(
        category -> category.getId() == categoryId).findFirst()
        .orElse(rootItem.getChildren().get(0).getValue());
    return selectedCategory;
  }

  /**
   * Selects the given category in the TreeView.
   *
   * @param category the category to be selected
   */
  public void setSelectedItem(Category category) {
    getSelectionModel().select(categoryTreeItemMap.get(category));
  }

  /**
   * Retrieves the currently selected category in the TreeView.
   */
  public Category getSelectedCategory() {
    TreeItem<Category> selectedTreeItem = (TreeItem<Category>) getSelectionModel().getSelectedItem();
    if (selectedTreeItem != null) {
      return ((TreeItem<Category>) getSelectionModel().getSelectedItem()).getValue();
    }
    return null;
  }
}
