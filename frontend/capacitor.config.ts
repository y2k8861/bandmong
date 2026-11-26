import type { CapacitorConfig } from '@capacitor/cli';

// iOS 앱 설정. 화면은 빌드한 React(dist)를 앱 안에 담고, API는 운영 서버(VITE_API_BASE)로 보낸다.
const config: CapacitorConfig = {
  appId: 'com.bandmong.app',
  appName: '밴드몽',
  webDir: 'dist',
  ios: {
    contentInset: 'never',
    backgroundColor: '#16181D',
  },
  plugins: {
    FirebaseMessaging: {
      // 앱을 보고 있을 때 온 푸시도 배너로 보여 준다
      presentationOptions: ['badge', 'sound', 'alert'],
    },
  },
};

export default config;
