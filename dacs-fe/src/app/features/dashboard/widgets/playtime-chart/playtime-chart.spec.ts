import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlaytimeChart } from './playtime-chart';

describe('PlaytimeChart', () => {
  let component: PlaytimeChart;
  let fixture: ComponentFixture<PlaytimeChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlaytimeChart]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlaytimeChart);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
