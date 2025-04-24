/**
 * 生成指定范围内的随机整数
 * @param {Number} min 最小值
 * @param {Number} max 最大值
 * @returns {Number} 生成的随机整数
 */
export function generateRandomValue(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }
  
  /**
   * 生成随机数据数组
   * @param {Number} length 数组长度
   * @param {Number} min 最小值
   * @param {Number} max 最大值
   * @returns {Array} 生成的随机数据数组
   */
  export function generateRandomData(length, min, max) {
    return Array.from({length}, () => generateRandomValue(min, max));
  }
  
  /**
   * 格式化数字为千分位分隔的字符串
   * @param {Number} num 要格式化的数字
   * @returns {String} 格式化后的字符串
   */
  export function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  }