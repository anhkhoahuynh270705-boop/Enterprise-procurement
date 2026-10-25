import { CreateRfqRequest } from './rfq.models';

export interface RfqFormSaveEvent {
  rfqId: string | null;
  request: CreateRfqRequest;
  submit: boolean;
}
