import { TestBed } from '@angular/core/testing';

import { DomainRepositoryService } from './domain-repository.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { formStateKey, initialFormState } from '../../state/form.state';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { applicationStateKey, initialApplicationState } from '../../../application/state/application.state';

describe('DomainRepositoryService', () => {
  const initialState = {
    [formStateKey]: initialFormState,
    [applicationStateKey]: initialApplicationState,
  };
  let store: MockStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [
        provideMockStore({ initialState }),
      ],
    });
    store = TestBed.inject(MockStore);
  });

  it('should be created', () => {
    const service: DomainRepositoryService = TestBed.inject(DomainRepositoryService);
    expect(service).toBeTruthy();
  });
});
