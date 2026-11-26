// 앱에서만 쓰는 기능 (Apple 로그인, 소셜 로그인 브라우저, 푸시). 웹에서는 부르지 않는다.
import { App } from '@capacitor/app';
import { Browser } from '@capacitor/browser';
import { SignInWithApple } from '@capacitor-community/apple-sign-in';
import { FirebaseMessaging } from '@capacitor-firebase/messaging';
import { appleLogin, exchangeCode, registerDevice, type Issued } from '../api/app';
import { apiBase, platform } from './platform';

/** Apple로 로그인 → identityToken을 서버가 검증하고 앱 토큰을 준다 */
export async function signInWithApple(): Promise<Issued> {
  const res = await SignInWithApple.authorize({
    clientId: 'com.bandmong.app',
    redirectURI: `${apiBase}/`,
    scopes: 'email',
  });
  return appleLogin(res.response.identityToken);
}

/**
 * 카카오·네이버·구글: 시스템 브라우저로 서버의 소셜 로그인을 열고,
 * 끝나면 서버가 bandmong://auth?code=… 로 돌려보낸다 → 코드를 토큰으로 바꾼다.
 */
export function signInWithProvider(provider: string): Promise<Issued> {
  return new Promise((resolve, reject) => {
    let done = false;
    const sub = App.addListener('appUrlOpen', async ({ url }) => {
      if (!url.startsWith('bandmong://auth')) return;
      done = true;
      (await sub).remove();
      await Browser.close().catch(() => undefined);
      const code = new URL(url.replace('bandmong://', 'https://x/')).searchParams.get('code');
      if (!code) return reject(new Error('로그인하지 못했어요. 다시 시도해 주세요'));
      exchangeCode(code).then(resolve, reject);
    });
    Browser.addListener('browserFinished', async () => {
      if (!done) {
        (await sub).remove();
        reject(new Error('로그인을 취소했어요'));
      }
    });
    Browser.open({ url: `${apiBase}/oauth2/authorization/${provider}?app=1`, presentationStyle: 'popover' });
  });
}

let pushToken: string | null = null;
export const currentPushToken = () => pushToken;

/** 로그인한 뒤 한 번: 알림 권한을 묻고 이 기기를 서버에 등록. 알림을 누르면 그 화면으로. */
export async function setupPush(navigate: (path: string) => void) {
  try {
    const perm = await FirebaseMessaging.requestPermissions();
    if (perm.receive !== 'granted') return;
    const { token } = await FirebaseMessaging.getToken();
    pushToken = token;
    await registerDevice(token, platform === 'android' ? 'ANDROID' : 'IOS');
    await FirebaseMessaging.removeAllListeners();
    await FirebaseMessaging.addListener('tokenReceived', (e) => {
      pushToken = e.token;
      registerDevice(e.token, platform === 'android' ? 'ANDROID' : 'IOS').catch(() => undefined);
    });
    await FirebaseMessaging.addListener('notificationActionPerformed', (e) => {
      const link = (e.notification.data as { link?: string } | undefined)?.link;
      if (link && link.startsWith('/')) navigate(link);
    });
  } catch {
    // 푸시는 없어도 앱은 동작한다 (Firebase 설정 전, 시뮬레이터 등)
  }
}
