{ Meu programa em Pascal }

const TAM = 10.0;

type vetor = array [15] of integer;

type aluno = record
  nota1: real;
  nota2: real;
end;

var A, B, C, D: integer;

var E: vetor;

var F: aluno;

var result: integer;

function fatorial(a: integer): integer
var
  i: integer;
begin
  i := 1;
  result := 1;
  while i < a
  begin
    result:=result*i;
    i := i + 1
  end
end

begin
  A := TAM + 20;
  B := result(A)
end