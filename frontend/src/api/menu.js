import { request } from './client'

export function getMenuTree() {
  return request('/api/menus/tree')
}
