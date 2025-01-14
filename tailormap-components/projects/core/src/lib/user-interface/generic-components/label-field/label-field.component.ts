import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { BaseFieldComponent } from '../base-field/base-field.component';

@Component({
  selector: 'tailormap-label-field',
  templateUrl: './label-field.component.html',
  styleUrls: ['./label-field.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LabelFieldComponent extends BaseFieldComponent implements OnInit {
  @Input()
  public valueTrue: string;
  constructor() {
    super();
  }

  public ngOnInit(): void {
  }

  public getLink() {
    if (!/^http/.test(this.value)) {
      return `//${this.value}`;
    }
    return this.value;
  }

  public hasDisplayValue() {
    return this.value !== null && this.value !== undefined && this.value !== '';
  }

}
