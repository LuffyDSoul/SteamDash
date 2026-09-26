export interface IResponse {
        error : boolean ;
        errorCode: string;
        errorDescription: string;
        data: any;
}
export interface ITestResponse
{
 id: number;
  name: string;
  age: number;
  city: string; // <-- ¡Añade esta línea!
}