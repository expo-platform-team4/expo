import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * 조건부 클래스 이름을 합치고 충돌하는 Tailwind 클래스를 정리한다.
 *
 * `cn('px-4', condition && 'px-6')` 처럼 써서 마지막에 이긴 값만 남긴다.
 * 이게 없으면 컴포넌트마다 클래스 문자열을 손으로 이어 붙이게 되고, 그 과정에서
 * 같은 역할의 컴포넌트가 조금씩 다르게 만들어진다 — 이 프로젝트가 피하려는 것이다.
 */
export const cn = (...inputs: ClassValue[]) => twMerge(clsx(inputs))
