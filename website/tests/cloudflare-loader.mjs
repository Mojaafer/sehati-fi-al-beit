const cloudflareWorkersShim = "data:text/javascript," + encodeURIComponent("export const env = {}; export default {};\n");

export async function resolve(specifier, context, nextResolve) {
  if (specifier === "cloudflare:workers") {
    return { url: cloudflareWorkersShim, shortCircuit: true };
  }
  return nextResolve(specifier, context);
}
