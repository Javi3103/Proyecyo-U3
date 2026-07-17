/**
 * Aplana el árbol de menú (Módulo -> Submenú -> Item) en la lista de nodos
 * hoja navegables. Un nodo es hoja navegable si trae `url` (los nodos
 * agrupadores de módulos/submenús tienen `url: null`, ver MenuNodoDto).
 */
export function buildRoutesFromMenu(tree) {
  const leaves = []

  function walk(nodes) {
    for (const node of nodes) {
      if (node.url) {
        leaves.push({ path: node.url, nombre: node.nombre, id: node.id })
      }
      if (node.hijos?.length) {
        walk(node.hijos)
      }
    }
  }

  walk(tree ?? [])
  return leaves
}
