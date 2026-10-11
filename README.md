GET
전체조회 : localhost:8080/api/test/items
단건조회 : localhost:8080/api/test/items/{id}  

POST
아이템생성 : localhost:8080/api/test/items 
헤더사용 아이템생성 : localhost:8080/api/test/items/with-header
예시용 body 
{"name":"keyboard","price":30000}
{"name":"mouse","price":5000}
{"name":"monitor","price":150000}
헤더
X-USER-ID (필수)
Authorization (선택)

PUT
이름,가격 수정 : localhost:8080/api/test/items/{id}
예시용 body (id=1)
{"name":"keyboard-pro","price":45000}
가격수정: localhost:8080/api/test/items/{id}/price
예시용 body
{"price":50000}

DELETE
전체삭제 : localhost:8080/api/test/items
단건삭제 : localhost:8080/api/test/items/{id}


사용코드
2xx: 200 (요청 성공), 201 (생성됨)
4xx: 400 (잘못된 요청), 404 (리소스 없음), 409 (이름 중복)
5xx: 500 (서버 내부 오류), 503 (서비스 일시 불가)

헤더에 X-Simulate-Error  value 500/503으로 5xx 시연
