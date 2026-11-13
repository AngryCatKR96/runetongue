package com.runehangul.text;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SkipReason
{
	TOO_FEW_COLUMNS("열이 2개보다 적음"),
	EMPTY_ENGLISH("english 칸이 비어 있음"),
	PLACEHOLDER_MISMATCH("치환 기호가 원문과 다름"),
	CHOICE_MISMATCH("[A/B] 선택지 개수가 맞지 않음"),
	BAD_ENCODING("글자 인코딩이 깨짐 (UTF-8 또는 UTF-16으로 저장)"),
	OVERRIDDEN("같은 파일의 뒤 줄이 덮어씀");

	private final String message;
}
