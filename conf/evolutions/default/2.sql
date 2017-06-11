# --- Sample dataset

# --- !Ups

--insert into bob (name, category, distance) values ('ONE', 'korean', 'near');
--insert into bob (name, category, distance) values ('TWO', 'korean', 'mid');
--insert into bob (name, category, distance) values ('THREE', 'korean', 'far');
--insert into bob (name, category, distance) values ('FOUR', 'japanese', 'near');
--insert into bob (name, category, distance) values ('FIVE', 'japanese', 'mid');
--insert into bob (name, category, distance) values ('SIX', 'japanese', 'far');
--insert into bob (name, category, distance) values ('SEVEN', 'chinese', 'near');
--insert into bob (name, category, distance) values ('EIGHT', 'chinese', 'mid');
--insert into bob (name, category, distance) values ('NINE', 'chinese', 'far');
--insert into bob (name, category, distance) values ('TEN', 'etc', 'near');
insert into bob (name, category, distance) values ('부산 아지매 국밥', 'korean', 'far');
insert into bob (name, category, distance) values ('딸부자네 불백', 'korean', 'mid');
insert into bob (name, category, distance) values ('선릉 설렁탕', 'korean', 'near');
insert into bob (name, category, distance) values ('선릉 해물집', 'korean', 'near');
insert into bob (name, category, distance) values ('일미리 금계찜닭', 'korean', 'near');
insert into bob (name, category, distance) values ('삼군 김치찌개', 'korean', 'near');
insert into bob (name, category, distance) values ('호타루', 'japanese', 'near');
insert into bob (name, category, distance) values ('TAN', 'japanese', 'mid');
insert into bob (name, category, distance) values ('이화수', 'korean', 'far');
insert into bob (name, category, distance) values ('버거킹', 'etc', 'far');
insert into bob (name, category, distance) values ('제주아강보쌈', 'korean', 'mid');
insert into bob (name, category, distance) values ('스시 마이우', 'japanese', 'near');
insert into bob (name, category, distance) values ('교동짬뽕', 'chinese', 'near');
insert into bob (name, category, distance) values ('채린', 'chinese', 'mid');
insert into bob (name, category, distance) values ('우찌노 카레', 'japanese', 'far');
insert into bob (name, category, distance) values ('서래향', 'chinese', 'mid');
insert into bob (name, category, distance) values ('청춘화로', 'korean', 'mid');
insert into bob (name, category, distance) values ('흑돈명가', 'korean', 'mid');
insert into bob (name, category, distance) values ('쉐프의 부대찌개', 'korean', 'mid');
insert into bob (name, category, distance) values ('육고탁', 'korean', 'mid');

# --- !Downs

delete from bob;