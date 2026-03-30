/**
 * 顶栏大搜索展开时隐藏「首页…管理」导航，多组件共享同一状态。
 */
export function useHeaderSearchExpand() {
  const headerSearchExpanded = useState("header-search-expanded", () => false);

  function openHeaderSearch() {
    headerSearchExpanded.value = true;
  }

  function closeHeaderSearch() {
    headerSearchExpanded.value = false;
  }

  return {
    headerSearchExpanded,
    openHeaderSearch,
    closeHeaderSearch,
  };
}
