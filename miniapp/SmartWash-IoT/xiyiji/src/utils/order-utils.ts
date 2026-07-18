/**
 * 订单工具类
 * 提供订单相关的辅助功能
 */

/**
 * 生成唯一订单号
 * 格式：YYYYMMDDHHmmssSSS + 4位随机数
 * @returns 唯一订单号
 */
export function generateOrderNo(): string {
  const date = new Date();
  const timestamp = date.toISOString().replace(/[^0-9]/g, '').slice(0, 17);
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0');
  return `${timestamp}${random}`;
}
