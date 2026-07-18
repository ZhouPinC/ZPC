Component({
  data: {
    selected: 0,
    color: "#999999",
    selectedColor: "#1296db",
    list: [
      {
        pagePath: "/pages/device/index",
        text: "设备",
        iconPath: "/images/device.png",
        selectedIconPath: "/images/device_active.png"
      },
      {
        pagePath: "/pages/add/index",
        text: "扫码",
        iconPath: "/images/scan.png",
        selectedIconPath: "/images/scan.png",
        isCenter: true
      },
      {
        pagePath: "/pages/me/index",
        text: "我的",
        iconPath: "/images/me.png",
        selectedIconPath: "/images/me_active.png"
      }
    ]
  },

  methods: {
    switchTab(e) {
      const data = e.currentTarget.dataset;
      const url = data.path;
      
      wx.switchTab({ url });
    }
  }
});
