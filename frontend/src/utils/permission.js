/**
 * 权限工具函数
 */

// 角色常量
export const ROLE_ADMIN = 'ADMIN'
export const ROLE_EDITOR = 'EDITOR'
export const ROLE_USER = 'USER'

// 权限常量
export const PERMISSION_VIEW = 'VIEW'
export const PERMISSION_UPLOAD = 'UPLOAD'
export const PERMISSION_EDIT = 'EDIT'
export const PERMISSION_DELETE = 'DELETE'
export const PERMISSION_DOWNLOAD = 'DOWNLOAD'
export const PERMISSION_AUDIT = 'AUDIT'
export const PERMISSION_SUBMIT_AUDIT = 'SUBMIT_AUDIT'
export const PERMISSION_VIEW_AUDIT = 'VIEW_AUDIT'
export const PERMISSION_MANAGE_USER = 'MANAGE_USER'
export const PERMISSION_MANAGE_ROLE = 'MANAGE_ROLE'
export const PERMISSION_MANAGE_CONFIG = 'MANAGE_CONFIG'
export const PERMISSION_MANAGE_STRUCTURE = 'MANAGE_STRUCTURE'
export const PERMISSION_VIEW_STATISTICS = 'VIEW_STATISTICS'
export const PERMISSION_EXPORT_DATA = 'EXPORT_DATA'
export const PERMISSION_ALL = 'ALL'

/**
 * 检查用户是否有指定权限
 * @param {Object} userInfo - 用户信息对象
 * @param {String} permission - 权限代码
 * @returns {Boolean}
 */
export function hasPermission(userInfo, permission) {
  if (!userInfo || !userInfo.permissions) {
    return false
  }
  
  // 如果有ALL权限，则拥有所有权限
  if (userInfo.permissions.includes(PERMISSION_ALL)) {
    return true
  }
  
  return userInfo.permissions.includes(permission)
}

/**
 * 检查用户是否有指定角色
 * @param {Object} userInfo - 用户信息对象
 * @param {String} role - 角色代码
 * @returns {Boolean}
 */
export function hasRole(userInfo, role) {
  if (!userInfo || !userInfo.role) {
    return false
  }
  return userInfo.role === role
}

/**
 * 检查用户是否有任意一个指定角色
 * @param {Object} userInfo - 用户信息对象
 * @param {Array} roles - 角色代码数组
 * @returns {Boolean}
 */
export function hasAnyRole(userInfo, roles) {
  if (!userInfo || !userInfo.role) {
    return false
  }
  return roles.includes(userInfo.role)
}

/**
 * 检查用户是否是管理员
 * @param {Object} userInfo - 用户信息对象
 * @returns {Boolean}
 */
export function isAdmin(userInfo) {
  return hasRole(userInfo, ROLE_ADMIN)
}

/**
 * 检查用户是否是编辑员
 * @param {Object} userInfo - 用户信息对象
 * @returns {Boolean}
 */
export function isEditor(userInfo) {
  return hasRole(userInfo, ROLE_EDITOR)
}

/**
 * 检查用户是否是普通用户
 * @param {Object} userInfo - 用户信息对象
 * @returns {Boolean}
 */
export function isUser(userInfo) {
  return hasRole(userInfo, ROLE_USER)
}

