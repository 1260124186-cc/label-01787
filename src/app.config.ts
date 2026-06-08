export default defineAppConfig({
  pages: [
    'pages/index/index',
    'pages/bookshelf/bookshelf',
    'pages/notes/notes',
    'pages/notifications/index',
    'pages/profile/profile',
    'pages/notifications-detail/index'
  ],
  window: {
    backgroundTextStyle: 'dark',
    navigationBarBackgroundColor: '#6B4226',
    navigationBarTitleText: '小安的书店',
    navigationBarTextStyle: 'white',
    backgroundColor: '#F5F0EB'
  },
  tabBar: {
    color: '#999999',
    selectedColor: '#6B4226',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/index/index',
        text: '首页'
      },
      {
        pagePath: 'pages/bookshelf/bookshelf',
        text: '书架'
      },
      {
        pagePath: 'pages/notes/notes',
        text: '笔记'
      },
      {
        pagePath: 'pages/notifications/index',
        text: '消息'
      },
      {
        pagePath: 'pages/profile/profile',
        text: '我的'
      }
    ]
  }
})
