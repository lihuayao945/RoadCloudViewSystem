// 导入默认的logo图片
import DefaultLogo from '@/assets/logo.png';

/**
 * 解析图片路径，处理特殊格式的路径
 * @param {string} path - 图片路径
 * @returns {string} - 处理后的图片路径
 */
export const resolveImagePath = (path) => {
  // 如果路径为空或不是字符串，返回默认图片
  if (!path || typeof path !== 'string') {
    console.warn('图片路径无效，使用默认图片', path);
    return DefaultLogo;
  }
  
  try {
    console.log('处理图片路径:', path);
    
    // 处理@路径 - 这里我们尝试将其转换为相对路径而不是使用默认图片
    if (path.startsWith('@/')) {
      // 移除@/前缀，替换为/src/
      const relativePath = path.replace('@/', '/src/');
      console.log('将@/路径转换为相对路径:', relativePath);
      return relativePath;
    }
    
    // 如果是相对路径但不是以/开头
    if (!path.startsWith('/') && 
        !path.startsWith('http') && 
        !path.startsWith('data:')) {
      const newPath = '/' + path;
      console.log('添加前导斜杠到路径:', newPath);
      return newPath;
    }
    
    // 如果是完整路径，直接返回
    return path;
  } catch (error) {
    console.error('解析图片路径出错:', error);
    return DefaultLogo;
  }
};

/**
 * 图片加载错误处理函数
 * @param {Event} event - 图片加载错误事件
 */
export const handleImageError = (event) => {
  console.warn('图片加载失败，使用默认图片');
  event.target.src = DefaultLogo;
};

// 尝试直接使用静态路径解析
export const getStaticImageUrl = (imageName) => {
  try {
    // 这里列出一些常用的图片路径，方便直接选择
    const staticImages = {
      'logo': DefaultLogo,
      // 可以添加更多静态图片
    };
    
    return staticImages[imageName] || DefaultLogo;
  } catch (error) {
    console.error('获取静态图片出错:', error);
    return DefaultLogo;
  }
};

export default {
  resolveImagePath,
  handleImageError,
  getStaticImageUrl,
  DefaultLogo
}; 