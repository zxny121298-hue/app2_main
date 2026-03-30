export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuth();

  if (!auth.isLoggedIn.value) {
    return navigateTo(`/login?redirect=${encodeURIComponent(to.fullPath)}`);
  }

  if (!auth.isAdmin.value) {
    return navigateTo('/');
  }
});
